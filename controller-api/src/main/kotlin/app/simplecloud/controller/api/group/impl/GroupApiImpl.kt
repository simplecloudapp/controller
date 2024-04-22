package app.simplecloud.controller.api.group.impl

import app.simplecloud.controller.api.Controller
import app.simplecloud.controller.api.group.GroupApi
import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.ControllerGroupServiceGrpc
import build.buf.gen.simplecloud.controller.v1.GetGroupByNameRequest
import app.simplecloud.controller.shared.status.ApiResponse
import java.util.concurrent.CompletableFuture

class GroupApiImpl(
    authCallCredentials: AuthCallCredentials
) : GroupApi {

    private val managedChannel = Controller.createManagedChannelFromEnv()

    private val groupServiceStub: ControllerGroupServiceGrpc.ControllerGroupServiceFutureStub =
        ControllerGroupServiceGrpc.newFutureStub(managedChannel)
            .withCallCredentials(authCallCredentials)

    override fun getGroupByName(name: String): CompletableFuture<Group> {
        return groupServiceStub.getGroupByName(
            GetGroupByNameRequest.newBuilder()
                .setName(name)
                .build()
        ).toCompletable()
            .thenApply {
                Group.fromDefinition(it.group)
            }
    }

    override fun deleteGroup(name: String): CompletableFuture<ApiResponse> {
        return groupServiceStub.deleteGroupByName(
            GetGroupByNameRequest.newBuilder()
                .setName(name)
                .build()
        ).toCompletable()
            .thenApply {
                ApiResponse.fromDefinition(it)
            }
    }

    override fun createGroup(group: Group): CompletableFuture<ApiResponse> {
        return groupServiceStub.createGroup(
            group.toDefinition()
        ).toCompletable()
            .thenApply {
                ApiResponse.fromDefinition(it)
            }
    }

    override fun getAllGroups(): CompletableFuture<List<Group>> {
        TODO("Not yet implemented")
    }

}