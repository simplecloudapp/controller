package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostException
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.Context
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture

class ServerService(
    private val numericalIdRepository: ServerNumericalIdRepository,
    private val serverRepository: ServerRepository,
    private val hostRepository: ServerHostRepository,
    private val groupRepository: GroupRepository,
    private val forwardingSecret: String,
    private val authCallCredentials: AuthCallCredentials
) : ControllerServerServiceGrpc.ControllerServerServiceImplBase() {

    private val logger = LogManager.getLogger(ServerService::class.java)

    override fun attachServerHost(request: ServerHostDefinition, responseObserver: StreamObserver<ServerHostDefinition>) {
        val serverHost = ServerHost.fromDefinition(request)
        try {
            hostRepository.delete(serverHost)
            hostRepository.save(serverHost)
        }catch (e: Exception) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Could not save serverhost")
                    .withCause(e)
                    .asRuntimeException()
            )
            return
        }
        logger.info("Successfully registered ServerHost ${serverHost.id}.")
        responseObserver.onNext(serverHost.toDefinition())
        responseObserver.onCompleted()
        Context.current().fork().run {
            val channel = serverHost.createChannel()
            val stub = ServerHostServiceGrpc.newFutureStub(channel)
                .withCallCredentials(authCallCredentials)
            serverRepository.findServersByHostId(serverHost.id).thenApply {
                it.forEach { server ->
                    logger.info("Reattaching Server ${server.uniqueId} of group ${server.group}...")
                    stub.reattachServer(server.toDefinition()).toCompletable().thenApply {
                        logger.info("Success!")
                    }.exceptionally {
                        logger.error("Server was found to be offline, unregistering...")
                        serverRepository.delete(server)
                    }.get()
                }
                channel.shutdown()
            }
        }
    }

    override fun getAllServers(
        request: GetAllServersRequest,
        responseObserver: StreamObserver<GetAllServersResponse>
    ) {
        serverRepository.getAll().thenApply { servers ->
            responseObserver.onNext(
                GetAllServersResponse.newBuilder()
                    .addAllServers(servers.map { it.toDefinition() })
                    .build()
            )
            responseObserver.onCompleted()
        }
    }

    override fun getServerByNumerical(
        request: GetServerByNumericalRequest,
        responseObserver: StreamObserver<ServerDefinition>
    ) {
        serverRepository.findServerByNumerical(request.group, request.numericalId.toInt()).thenApply { server ->
            if (server == null) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("No server was found matching this group and numerical id")
                        .asRuntimeException()
                )
                return@thenApply
            }
            responseObserver.onNext(server.toDefinition())
            responseObserver.onCompleted()
        }
    }

    override fun stopServerByNumerical(
        request: StopServerByNumericalRequest,
        responseObserver: StreamObserver<ServerDefinition>
    ) {

        serverRepository.findServerByNumerical(request.group, request.numericalId.toInt()).thenApply { server ->
            if (server == null) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("No server was found matching this group and numerical id")
                        .asRuntimeException()
                )
                return@thenApply
            }
            stopServer(server.toDefinition()).thenApply {
                responseObserver.onNext(it)
                responseObserver.onCompleted()
            }.exceptionally {
                responseObserver.onError(it)
            }
        }

    }

    override fun updateServer(request: ServerUpdateRequest, responseObserver: StreamObserver<ServerDefinition>) {
        val deleted = request.deleted
        val server = Server.fromDefinition(request.server)
        if (!deleted) {
            try {
                serverRepository.save(server)
            }catch (e: Exception) {
                responseObserver.onError(
                    Status.INTERNAL
                        .withDescription("Could not update server")
                        .withCause(e)
                        .asRuntimeException()
                )
                return
            }
            responseObserver.onNext(server.toDefinition())
            responseObserver.onCompleted()
        } else {
            logger.info("Deleting server ${server.uniqueId} of group ${request.server.groupName}...")
            serverRepository.delete(server).thenApply thenDelete@ {
                if(!it) {
                    responseObserver.onError(
                        Status.INTERNAL
                            .withDescription("Could not delete server")
                            .asRuntimeException()
                    )
                    return@thenDelete
                }
                logger.info("Deleted server ${server.uniqueId} of group ${request.server.groupName}.")
                responseObserver.onNext(server.toDefinition())
                responseObserver.onCompleted()
            }.exceptionally {
                responseObserver.onError(
                    Status.INTERNAL
                        .withDescription("Could not delete server")
                        .withCause(it)
                        .asRuntimeException()
                )
            }
        }
    }

    override fun getServerById(request: ServerIdRequest, responseObserver: StreamObserver<ServerDefinition>) {
        serverRepository.find(request.id).thenApply { server ->
            if (server == null) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("No server was found matching this unique id")
                        .asRuntimeException()
                )
                return@thenApply
            }
            responseObserver.onNext(server.toDefinition())
            responseObserver.onCompleted()
        }

    }

    override fun getServersByGroup(
        request: GroupNameRequest,
        responseObserver: StreamObserver<GetServersByGroupResponse>
    ) {
        serverRepository.findServersByGroup(request.name).thenApply { servers ->
            val response = GetServersByGroupResponse.newBuilder()
                .addAllServers(servers.map { it.toDefinition() })
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    override fun getServersByType(
        request: ServerTypeRequest,
        responseObserver: StreamObserver<GetServersByGroupResponse>
    ) {
        serverRepository.findServersByType(request.type).thenApply { servers ->
            val response = GetServersByGroupResponse.newBuilder()
                .addAllServers(servers.map { it.toDefinition() })
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    override fun startServer(request: GroupNameRequest, responseObserver: StreamObserver<ServerDefinition>) {
        hostRepository.find(serverRepository).thenApply { host ->
            if (host == null) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("No server host found, could not start server")
                        .asRuntimeException()
                )
                return@thenApply
            }
            groupRepository.find(request.name).thenApply { group ->
                if (group == null) {
                    responseObserver.onError(
                        Status.NOT_FOUND
                            .withDescription("No group was found matching this name")
                            .asRuntimeException()
                    )
                } else {
                    startServer(host, group)
                }
            }.exceptionally {
                logger.error("Error whilst starting server:", it)
            }
        }
    }

    private fun startServer(host: ServerHost, group: Group): CompletableFuture<ServerDefinition> {
        val numericalId = numericalIdRepository.findNextNumericalId(group.name)
        val server = buildServer(group, numericalId, forwardingSecret)
        serverRepository.save(server)
        val channel = host.createChannel()
        val stub = ServerHostServiceGrpc.newFutureStub(channel)
            .withCallCredentials(authCallCredentials)
        serverRepository.save(server)
        return stub.startServer(
            StartServerRequest.newBuilder()
                .setGroup(group.toDefinition())
                .setServer(server.toDefinition())
                .build()
        ).toCompletable().thenApply {
            serverRepository.save(Server.fromDefinition(it))
            channel.shutdown()
            return@thenApply it
        }.exceptionally {
            serverRepository.delete(server)
            numericalIdRepository.removeNumericalId(group.name, server.numericalId)
            channel.shutdown()
            logger.error("Error whilst starting server:", it)
            throw it
        }
    }

    private fun buildServer(group: Group, numericalId: Int, forwardingSecret: String): Server {
        return Server.fromDefinition(
            ServerDefinition.newBuilder()
                .setNumericalId(numericalId)
                .setType(group.type)
                .setGroupName(group.name)
                .setMinimumMemory(group.minMemory)
                .setMaximumMemory(group.maxMemory)
                .setState(ServerState.PREPARING)
                .setMaxPlayers(group.maxPlayers)
                .setCreatedAt(LocalDateTime.now().toString())
                .setUpdatedAt(LocalDateTime.now().toString())
                .setPlayerCount(0)
                .setUniqueId(UUID.randomUUID().toString().replace("-", "")).putAllProperties(
                    mapOf(
                        *group.properties.entries.map { it.key to it.value }.toTypedArray(),
                        "forwarding-secret" to forwardingSecret,
                    )
                ).build()
        )
    }

    override fun stopServer(request: ServerIdRequest, responseObserver: StreamObserver<ServerDefinition>) {
        serverRepository.find(request.id).thenApply { server ->
            if (server == null) {
                throw Status.NOT_FOUND
                    .withDescription("No server was found matching this id.")
                    .asRuntimeException()
            }
            stopServer(server.toDefinition()).thenApply {
                responseObserver.onNext(it)
                responseObserver.onCompleted()
            }.get()
        }.exceptionally {
            logger.error("Error whilst stopping server:", it)
            responseObserver.onError(it)
        }
    }

    private fun stopServer(server: ServerDefinition): CompletableFuture<ServerDefinition> {
        val host = hostRepository.findServerHostById(server.hostId)
            ?: throw Status.NOT_FOUND
                .withDescription("No server host was found matching this server.")
                .asRuntimeException()
        val channel = host.createChannel()
        val stub = ServerHostServiceGrpc.newFutureStub(channel)
            .withCallCredentials(authCallCredentials)
        return stub.stopServer(server).toCompletable().thenApply {
            serverRepository.delete(Server.fromDefinition(server))
            channel.shutdown()
            return@thenApply it
        }.exceptionally {
            logger.error("Server stop error occured:", it)
            throw it
        }
    }

    override fun updateServerProperty(
        request: ServerUpdatePropertyRequest,
        responseObserver: StreamObserver<ServerDefinition>
    ) {
        serverRepository.find(request.id).thenApply { server ->
            if (server == null) {
                throw Status.NOT_FOUND
                    .withDescription("Server with id ${request.id} does not exist.")
                    .asRuntimeException()
            }
            server.properties[request.key] = request.value
            serverRepository.save(server)
            responseObserver.onNext(server.toDefinition())
            responseObserver.onCompleted()
        }.exceptionally {
            responseObserver.onError(it)
        }
    }

    override fun updateServerState(
        request: ServerUpdateStateRequest,
        responseObserver: StreamObserver<ServerDefinition>
    ) {
        serverRepository.find(request.id).thenApply { server ->
            if (server == null) {
                throw Status.NOT_FOUND
                    .withDescription("Server with id ${request.id} does not exist.")
                    .asRuntimeException()
            }
            server.state = request.state
            serverRepository.save(server)
            responseObserver.onNext(server.toDefinition())
            responseObserver.onCompleted()
        }.exceptionally {
            responseObserver.onError(it)
        }
    }

}