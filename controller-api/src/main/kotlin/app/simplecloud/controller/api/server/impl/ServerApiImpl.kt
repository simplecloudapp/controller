package app.simplecloud.controller.api.server.impl

import app.simplecloud.controller.api.Controller
import app.simplecloud.controller.api.server.ServerApi
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.ControllerServerServiceGrpc
import app.simplecloud.controller.shared.proto.GroupNameRequest
import app.simplecloud.controller.shared.proto.ServerIdRequest
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.status.ApiResponse
import com.google.protobuf.Api
import java.util.concurrent.CompletableFuture

class ServerApiImpl : ServerApi {

    private val messageChannel = Controller.createManagedChannelFromEnv()

    private val serverServiceStub: ControllerServerServiceGrpc.ControllerServerServiceFutureStub =
            ControllerServerServiceGrpc.newFutureStub(messageChannel)

    override fun getServerById(id: String): CompletableFuture<Server> {
        return serverServiceStub.getServerById(
                ServerIdRequest.newBuilder()
                        .setId(id)
                        .build()
        ).toCompletable()
                .thenApply {
                    Server.fromDefinition(it)
                }
    }

    override fun getServersByGroup(groupName: String): CompletableFuture<List<Server>> {
        return serverServiceStub.getServersByGroup(
                GroupNameRequest.newBuilder()
                        .setName(groupName)
                        .build()
        ).toCompletable()
                .thenApply {
                    Server.fromDefinition(it.serversList)
                }
    }

    override fun getServersByGroup(group: Group): CompletableFuture<List<Server>> {
        return getServersByGroup(group.name)
    }

    override fun startServer(groupName: String): CompletableFuture<Server?> {
        return serverServiceStub.startServer(
                GroupNameRequest.newBuilder()
                        .setName(groupName)
                        .build()
        ).toCompletable()
                .thenApply {
                    Server.fromDefinition(it)
                }
    }

    override fun stopServer(id: String): CompletableFuture<ApiResponse> {
        return serverServiceStub.stopServer(
                ServerIdRequest.newBuilder()
                        .setId(id)
                        .build()
        ).toCompletable()
                .thenApply {
                    ApiResponse.fromDefinition(it)
                }
    }
}