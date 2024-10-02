package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.time.ProtoBufTimestamp
import app.simplecloud.pubsub.PubSubClient
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
    private val authCallCredentials: AuthCallCredentials,
    private val pubSubClient: PubSubClient,
) : ControllerServerServiceGrpc.ControllerServerServiceImplBase() {

    private val logger = LogManager.getLogger(ServerService::class.java)

    override fun attachServerHost(
        request: AttachServerHostRequest,
        responseObserver: StreamObserver<ServerHostDefinition>
    ) {
        val serverHost = ServerHost.fromDefinition(request.serverHost)
        try {
            hostRepository.delete(serverHost)
            hostRepository.save(serverHost)
        } catch (e: Exception) {
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
        serverRepository.findServerByNumerical(request.groupName, request.numericalId.toInt()).thenApply { server ->
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

        serverRepository.findServerByNumerical(request.groupName, request.numericalId.toInt()).thenApply { server ->
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

    override fun updateServer(request: UpdateServerRequest, responseObserver: StreamObserver<ServerDefinition>) {
        val deleted = request.deleted
        val server = Server.fromDefinition(request.server)
        if (!deleted) {
            val before: Server
            try {
                before = serverRepository.find(server.uniqueId).resultNow()!!
                serverRepository.save(server)
            } catch (e: Exception) {
                responseObserver.onError(
                    Status.INTERNAL
                        .withDescription("Could not update server")
                        .withCause(e)
                        .asRuntimeException()
                )
                return
            }
            pubSubClient.publish("event",
                ServerUpdateEvent.newBuilder().setUpdatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                    .setServerBefore(before.toDefinition()).setServerAfter(request.server).build()
            )
            responseObserver.onNext(server.toDefinition())
            responseObserver.onCompleted()
        } else {
            logger.info("Deleting server ${server.uniqueId} of group ${request.server.groupName}...")
            serverRepository.delete(server).thenApply thenDelete@{
                if (!it) {
                    responseObserver.onError(
                        Status.INTERNAL
                            .withDescription("Could not delete server")
                            .asRuntimeException()
                    )
                    return@thenDelete
                }
                logger.info("Deleted server ${server.uniqueId} of group ${request.server.groupName}.")
                pubSubClient.publish(
                    "event", ServerStopEvent.newBuilder()
                        .setServer(request.server)
                        .setStoppedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                        .setStopCause(ServerStopCause.NATURAL_STOP)
                        .setTerminationMode(ServerTerminationMode.UNKNOWN_MODE) //TODO: Add proto fields to make changing this possible
                        .build()
                )
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

    override fun getServerById(request: GetServerByIdRequest, responseObserver: StreamObserver<ServerDefinition>) {
        serverRepository.find(request.serverId).thenApply { server ->
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
        request: GetServersByGroupRequest,
        responseObserver: StreamObserver<GetServersByGroupResponse>
    ) {
        serverRepository.findServersByGroup(request.groupName).thenApply { servers ->
            val response = GetServersByGroupResponse.newBuilder()
                .addAllServers(servers.map { it.toDefinition() })
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    override fun getServersByType(
        request: ServerTypeRequest,
        responseObserver: StreamObserver<GetServersByTypeResponse>
    ) {
        serverRepository.findServersByType(request.serverType).thenApply { servers ->
            val response = GetServersByTypeResponse.newBuilder()
                .addAllServers(servers.map { it.toDefinition() })
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    override fun startServer(
        request: ControllerStartServerRequest,
        responseObserver: StreamObserver<ServerDefinition>
    ) {
        hostRepository.find(serverRepository).thenApply { host ->
            if (host == null) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("No server host found, could not start server")
                        .asRuntimeException()
                )
                return@thenApply
            }
            groupRepository.find(request.groupName).thenApply { group ->
                if (group == null) {
                    responseObserver.onError(
                        Status.NOT_FOUND
                            .withDescription("No group was found matching this name")
                            .asRuntimeException()
                    )
                } else {
                    startServer(host, group).thenApply {
                        pubSubClient.publish(
                            "event", ServerStartEvent.newBuilder()
                                .setServer(it)
                                .setStartedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                                .setStartCause(request.startCause)
                                .build()
                        )
                    }
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
            ServerHostStartServerRequest.newBuilder()
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
                .setServerType(group.type)
                .setGroupName(group.name)
                .setMinimumMemory(group.minMemory)
                .setMaximumMemory(group.maxMemory)
                .setServerState(ServerState.PREPARING)
                .setMaxPlayers(group.maxPlayers)
                .setCreatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setUpdatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setPlayerCount(0)
                .setUniqueId(UUID.randomUUID().toString().replace("-", "")).putAllCloudProperties(
                    mapOf(
                        *group.properties.entries.map { it.key to it.value }.toTypedArray(),
                        "forwarding-secret" to forwardingSecret,
                    )
                ).build()
        )
    }

    override fun stopServer(request: StopServerRequest, responseObserver: StreamObserver<ServerDefinition>) {
        serverRepository.find(request.serverId).thenApply { server ->
            if (server == null) {
                throw Status.NOT_FOUND
                    .withDescription("No server was found matching this id.")
                    .asRuntimeException()
            }
            stopServer(server.toDefinition()).thenApply {
                pubSubClient.publish(
                    "event", ServerStopEvent.newBuilder()
                        .setServer(it)
                        .setStoppedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                        .setStopCause(request.stopCause)
                        .setTerminationMode(ServerTerminationMode.UNKNOWN_MODE) //TODO: Add proto fields to make changing this possible
                        .build()
                )
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
        request: UpdateServerPropertyRequest,
        responseObserver: StreamObserver<ServerDefinition>
    ) {
        serverRepository.find(request.serverId).thenApply { server ->
            if (server == null) {
                throw Status.NOT_FOUND
                    .withDescription("Server with id ${request.serverId} does not exist.")
                    .asRuntimeException()
            }
            val serverBefore = server.copy()
            server.properties[request.propertyKey] = request.propertyValue
            serverRepository.save(server)
            pubSubClient.publish(
                "event",
                ServerUpdateEvent.newBuilder().setUpdatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                    .setServerBefore(serverBefore.toDefinition()).setServerAfter(server.toDefinition()).build()
            )
            responseObserver.onNext(server.toDefinition())
            responseObserver.onCompleted()
        }.exceptionally {
            responseObserver.onError(it)
        }
    }

    override fun updateServerState(
        request: UpdateServerStateRequest,
        responseObserver: StreamObserver<ServerDefinition>
    ) {
        serverRepository.find(request.serverId).thenApply { server ->
            if (server == null) {
                throw Status.NOT_FOUND
                    .withDescription("Server with id ${request.serverState} does not exist.")
                    .asRuntimeException()
            }
            val serverBefore = server.copy()
            server.state = request.serverState
            serverRepository.save(server)
            pubSubClient.publish(
                "event",
                ServerUpdateEvent.newBuilder().setUpdatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                    .setServerBefore(serverBefore.toDefinition()).setServerAfter(server.toDefinition()).build()
            )
            responseObserver.onNext(server.toDefinition())
            responseObserver.onCompleted()
        }.exceptionally {
            responseObserver.onError(it)
        }
    }

}