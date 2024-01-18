package app.simplecloud.controller.api.impl

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.ControllerGroupServiceGrpc
import app.simplecloud.controller.shared.proto.GetGroupByNameRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.CompletableFuture

class ControllerApiImpl : ControllerApi {

    private val managedChannel = createManagedChannelFromEnv()

    protected val groupServiceStub: ControllerGroupServiceGrpc.ControllerGroupServiceFutureStub =
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

    private fun createManagedChannelFromEnv(): ManagedChannel {
        val host = System.getenv("CONTROLLER_HOST") ?: "127.0.0.1"
        val port = System.getenv("CONTROLLER_PORT")?.toInt() ?: 5816
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }

}