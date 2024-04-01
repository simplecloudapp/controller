package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostException
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.proto.*
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.server.ServerFactory
import app.simplecloud.controller.shared.status.ApiResponse
import io.grpc.Context
import io.grpc.stub.StreamObserver
import org.apache.logging.log4j.LogManager

class ServerService(
  private val numericalIdRepository: ServerNumericalIdRepository,
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
      val channel = serverHost.createChannel()
      val stub = ServerHostServiceGrpc.newFutureStub(channel)
      serverRepository.filter { it.host == serverHost.id }.forEach {
        logger.info("Reattaching Server ${it.uniqueId} of group ${it.group}...")
        stub.reattachServer(it.toDefinition()).toCompletable().thenApply { response ->
          val status = ApiResponse.fromDefinition(response)
          if (status.status == "success") {
            logger.info("Success!")
          } else {
            logger.error("Server was found to be offline, unregistering...")
            serverRepository.delete(it)
          }
          channel.shutdown()
        }
      }
    }
  }

  override fun updateServer(request: ServerUpdateRequest, responseObserver: StreamObserver<StatusResponse>) {
    val deleted = request.deleted
    val server = Server.fromDefinition(request.server)
    if (!deleted) {
      serverRepository.save(server)
      responseObserver.onNext(ApiResponse("success").toDefinition())
      responseObserver.onCompleted()
    } else {
      logger.info("Deleting server ${server.uniqueId} of group ${request.server.groupName}...")
      serverRepository.delete(server).thenApply {
        logger.info("Deleted server ${server.uniqueId} of group ${request.server.groupName}.")
        responseObserver.onNext(ApiResponse("success").toDefinition())
        responseObserver.onCompleted()
      }.exceptionally {
        responseObserver.onNext(ApiResponse("error").toDefinition())
        responseObserver.onCompleted()
      }
    }
  }

  override fun getServerById(request: ServerIdRequest, responseObserver: StreamObserver<ServerDefinition>) {
    val server = serverRepository.findServerById(request.id)?.toDefinition()
    responseObserver.onNext(server)
    responseObserver.onCompleted()
  }

  override fun getServersByGroup(
    request: GroupNameRequest,
    responseObserver: StreamObserver<GetServersByGroupResponse>
  ) {
    val servers = serverRepository.findServersByGroup(request.name).map { it.toDefinition() }
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
    val channel = host.createChannel()
    val stub = ServerHostServiceGrpc.newFutureStub(channel)
    val group = groupRepository.find(request.name)
    if (group == null) {
      responseObserver.onError(IllegalArgumentException("No group was found matching the group name."))
      return
    }

    val numericalId = numericalIdRepository.findNextNumericalId(group.name)
    val server = ServerFactory.builder()
      .setGroup(group)
      .setNumericalId(numericalId.toLong())
      .build()
    serverRepository.save(server)
    stub.startServer(
      StartServerRequest.newBuilder()
        .setGroup(group.toDefinition())
        .setServer(server.toDefinition())
        .build()
    ).toCompletable().thenApply {
      serverRepository.save(Server.fromDefinition(it))
      responseObserver.onNext(it)
      responseObserver.onCompleted()
      channel.shutdown()
      return@thenApply
    }.exceptionally {
      serverRepository.delete(server)
      numericalIdRepository.removeNumericalId(group.name, numericalId)
      responseObserver.onError(ServerHostException("Could not start server, aborting."))
      channel.shutdown()
    }
  }

  override fun stopServer(request: ServerIdRequest, responseObserver: StreamObserver<StatusResponse>) {
    val server = serverRepository.findServerById(request.id)?.toDefinition()
    if (server == null) {
      responseObserver.onError(IllegalArgumentException("No server was found matching this id."))
      return
    }
    val host = hostRepository.findServerHostById(server.hostId)
    if (host == null) {
      responseObserver.onError(ServerHostException("No server host was found matching this server."))
      return
    }
    val channel = host.createChannel()
    val stub = ServerHostServiceGrpc.newFutureStub(channel)
    stub.stopServer(server).toCompletable().thenApply {
      if (it.status == "success") {
        serverRepository.delete(Server.fromDefinition(server))
      }
      responseObserver.onNext(it)
      responseObserver.onCompleted()
      channel.shutdown()
    }
  }

}