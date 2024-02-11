package app.simplecloud.controller.api.group.impl

import app.simplecloud.controller.api.Controller
import app.simplecloud.controller.api.group.GroupApi
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.ControllerGroupServiceGrpc
import app.simplecloud.controller.shared.proto.GetGroupByNameRequest
import java.util.concurrent.CompletableFuture

class GroupApiImpl : GroupApi {

    private val managedChannel = Controller.createManagedChannelFromEnv()

    private val groupServiceStub: ControllerGroupServiceGrpc.ControllerGroupServiceFutureStub =
            ControllerGroupServiceGrpc.newFutureStub(managedChannel)

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

}