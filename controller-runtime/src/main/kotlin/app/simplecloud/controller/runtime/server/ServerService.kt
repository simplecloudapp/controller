package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.host.ServerHostException
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.proto.*
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.status.ApiResponse
import io.grpc.stub.StreamObserver

class ServerService(
        private val serverRepository: ServerRepository,
        private val hostRepository: ServerHostRepository,
        private val groupRepository: GroupRepository,
) : ControllerServerServiceGrpc.ControllerServerServiceImplBase() {

    private var serversToAttach = mutableListOf<Server>()

    init {
        serversToAttach.addAll(serverRepository)
    }

    override fun attachServerHost(request: ServerHostDefinition, responseObserver: StreamObserver<StatusResponse>) {
        val serverHost = ServerHost.fromDefinition(request)
        if(hostRepository.findServerHostById(serverHost.id) != null) {
            responseObserver.onNext(ApiResponse("error").toDefinition())
            responseObserver.onCompleted()
            return
        }
        hostRepository.add(serverHost)
        val stub = ServerHostServiceGrpc.newFutureStub(serverHost.endpoint)
        serversToAttach.filter { it.host == serverHost.id }.forEach {
            stub.reattachServer(it.toDefinition())
        }
        responseObserver.onNext(ApiResponse("success").toDefinition())
        responseObserver.onCompleted()
    }

    override fun getServerById(request: ServerIdRequest, responseObserver: StreamObserver<ServerDefinition>) {
        val server = serverRepository.findServerById(request.id)
        responseObserver.onNext(server)
        responseObserver.onCompleted()
    }

    override fun getServersByGroup(request: GroupNameRequest, responseObserver: StreamObserver<GetServersByGroupResponse>) {
        val servers = serverRepository.findServersByGroup(request.name)
        val response = GetServersByGroupResponse.newBuilder().addAllServers(servers).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun startServer(request: GroupNameRequest, responseObserver: StreamObserver<ServerDefinition>) {
        val host = hostRepository.findLaziestServerHost(serverRepository)
        if (host == null) {
            responseObserver.onError(ServerHostException("No server host found, could not start server."))
            responseObserver.onCompleted()
            return;
        }
        val stub = ServerHostServiceGrpc.newFutureStub(host.endpoint)
        val groupDefinition = groupRepository.findGroupByName(request.name)
        if (groupDefinition == null) {
            responseObserver.onError(IllegalArgumentException("No group was found matching the group name."))
            responseObserver.onCompleted()
            return;
        }
        stub.startServer(StartServerRequest.newBuilder()
            .setGroup(groupDefinition)
            .setNumericalId(serverRepository.findServersByGroup(groupDefinition.name).size + 1).build()).toCompletable().thenApply {
            serverRepository.save(Server.fromDefinition(it))
            responseObserver.onNext(it)
            responseObserver.onCompleted()
        }.exceptionally {
            responseObserver.onError(ServerHostException("Could not start server, aborting."))
            responseObserver.onCompleted()
        }
    }

    override fun stopServer(request: ServerIdRequest, responseObserver: StreamObserver<StatusResponse>) {
        val server = serverRepository.findServerById(request.id)
        if (server == null) {
            responseObserver.onError(IllegalArgumentException("No server was found matching this id."))
            responseObserver.onCompleted()
            return
        }
        val host = hostRepository.findServerHostById(server.hostId)
        if (host == null) {
            responseObserver.onError(ServerHostException("No server host was found matching this server."))
            responseObserver.onCompleted()
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