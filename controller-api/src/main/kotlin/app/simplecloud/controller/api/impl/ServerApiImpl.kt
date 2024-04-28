package app.simplecloud.controller.api.impl

import app.simplecloud.controller.api.ServerApi
import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.*
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.status.ApiResponse
import io.grpc.ManagedChannel
import java.util.concurrent.CompletableFuture

class ServerApiImpl(
    managedChannel: ManagedChannel,
    authCallCredentials: AuthCallCredentials
) : ServerApi {

    private val serverServiceStub: ControllerServerServiceGrpc.ControllerServerServiceFutureStub =
        ControllerServerServiceGrpc.newFutureStub(managedChannel).withCallCredentials(authCallCredentials)

    override fun getAllServers(): CompletableFuture<List<Server>> {
        return serverServiceStub.getAllServers(GetAllServersRequest.newBuilder().build()).toCompletable().thenApply {
            Server.fromDefinition(it.serversList)
        }
    }

    override fun getServerById(id: String): CompletableFuture<Server> {
        return serverServiceStub.getServerById(
            ServerIdRequest.newBuilder().setId(id).build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it)
        }
    }

    override fun getServersByGroup(groupName: String): CompletableFuture<List<Server>> {
        return serverServiceStub.getServersByGroup(
            GroupNameRequest.newBuilder().setName(groupName).build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it.serversList)
        }
    }

    override fun getServersByGroup(group: Group): CompletableFuture<List<Server>> {
        return getServersByGroup(group.name)
    }

    override fun getServersByType(type: ServerType): CompletableFuture<List<Server>> {
        return serverServiceStub.getServersByType(
            ServerTypeRequest.newBuilder().setType(type).build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it.serversList)
        }
    }

    override fun startServer(groupName: String): CompletableFuture<Server?> {
        return serverServiceStub.startServer(
            GroupNameRequest.newBuilder().setName(groupName).build()
        ).toCompletable().thenApply {
            Server.fromDefinition(it)
        }
    }

    override fun stopServer(groupName: String, numericalId: Long): CompletableFuture<ApiResponse> {
        return serverServiceStub.stopServerByNumerical(
            StopServerByNumericalRequest.newBuilder().setGroup(groupName).setNumericalId(numericalId).build()
        ).toCompletable().thenApply {
            ApiResponse.fromDefinition(it)
        }
    }

    override fun stopServer(id: String): CompletableFuture<ApiResponse> {
        return serverServiceStub.stopServer(
            ServerIdRequest.newBuilder().setId(id).build()
        ).toCompletable().thenApply {
            ApiResponse.fromDefinition(it)
        }
    }

    override fun updateServerState(id: String, state: ServerState): CompletableFuture<ApiResponse> {
        return serverServiceStub.updateServerState(
            ServerUpdateStateRequest.newBuilder().setState(state).setId(id).build()
        ).toCompletable().thenApply {
            return@thenApply ApiResponse.fromDefinition(it)
        }
    }

    override fun updateServerProperty(id: String, key: String, value: Any): CompletableFuture<ApiResponse> {
        return serverServiceStub.updateServerProperty(
            ServerUpdatePropertyRequest.newBuilder().setKey(key).setValue(value.toString()).setId(id).build()
        ).toCompletable().thenApply {
            return@thenApply ApiResponse.fromDefinition(it)
        }
    }
}