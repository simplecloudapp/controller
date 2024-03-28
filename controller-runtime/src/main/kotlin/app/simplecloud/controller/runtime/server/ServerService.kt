package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.host.ServerHostException
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.proto.*
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.server.ServerFactory
import app.simplecloud.controller.shared.status.ApiResponse
import io.grpc.Context
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import org.apache.logging.log4j.LogManager
import java.util.*

class ServerService(
    private val serverRepository: ServerRepository,
    private val hostRepository: ServerHostRepository,
    private val groupRepository: GroupRepository,
) : ControllerServerServiceGrpc.ControllerServerServiceImplBase() {

    private val logger = LogManager.getLogger(ServerService::class.java)

    override fun attachServerHost(request: ServerHostDefinition, responseObserver: StreamObserver<StatusResponse>) {
        val serverHost = ServerHost.fromDefinition(request)
        hostRepository.remove(serverHost)
        hostRepository.add(serverHost)
        logger.info("Successfully registered ServerHost ${serverHost.id}.")
        responseObserver.onNext(ApiResponse("success").toDefinition())
        responseObserver.onCompleted()
        Context.current().fork().run {
            val stub = ServerHostServiceGrpc.newFutureStub(serverHost.endpoint)
            serverRepository.filter { it.host == serverHost.id }.forEach {
                logger.info("Reattaching Server ${it.uniqueId} of group ${it.group}...")
                val status = ApiResponse.fromDefinition(stub.reattachServer(it.toDefinition()).toCompletable().get())
                if (status.status == "success") {
                    logger.info("Success!")
                } else {
                    logger.error("Server was found to be offline, unregistering...")
                    serverRepository.delete(it)
                }
            }
        }

    }

    override fun getServerById(request: ServerIdRequest, responseObserver: StreamObserver<ServerDefinition>) {
        val server = serverRepository.findServerById(request.id)
        responseObserver.onNext(server)
        responseObserver.onCompleted()
    }

    override fun getServersByGroup(
        request: GroupNameRequest,
        responseObserver: StreamObserver<GetServersByGroupResponse>
    ) {
        val servers = serverRepository.findServersByGroup(request.name)
        val response = GetServersByGroupResponse.newBuilder().addAllServers(servers).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun startServer(request: GroupNameRequest, responseObserver: StreamObserver<ServerDefinition>) {
        val host = hostRepository.findLaziestServerHost(serverRepository)
        if (host == null) {
            responseObserver.onError(ServerHostException("No server host found, could not start server."))
            return
        }
        val stub = ServerHostServiceGrpc.newFutureStub(host.endpoint)
        val groupDefinition = groupRepository.findGroupByName(request.name)
        if (groupDefinition == null) {
            responseObserver.onError(IllegalArgumentException("No group was found matching the group name."))
            return
        }
        val server = ServerFactory.builder()
            .setGroup(Group.fromDefinition(groupDefinition))
            .setNumericalId(serverRepository.findNextNumericalId(groupDefinition.name).toLong())
            .build()
        serverRepository.save(server)
        stub.startServer(server.toDefinition()).toCompletable().thenApply {
            serverRepository.save(Server.fromDefinition(it))
            responseObserver.onNext(it)
            responseObserver.onCompleted()
            return@thenApply
        }.exceptionally {
            serverRepository.delete(server)
            responseObserver.onError(ServerHostException("Could not start server, aborting."))
        }
    }

    override fun stopServer(request: ServerIdRequest, responseObserver: StreamObserver<StatusResponse>) {
        val server = serverRepository.findServerById(request.id)
        if (server == null) {
            responseObserver.onError(IllegalArgumentException("No server was found matching this id."))
            return
        }
        val host = hostRepository.findServerHostById(server.hostId)
        if (host == null) {
            responseObserver.onError(ServerHostException("No server host was found matching this server."))
            return
        }
        val stub = ServerHostServiceGrpc.newFutureStub(host.endpoint)
        stub.stopServer(server).toCompletable().thenApply {
            if (it.status == "success") {
                serverRepository.delete(Server.fromDefinition(server))
            }
            responseObserver.onNext(it)
            responseObserver.onCompleted()
        }
    }

}