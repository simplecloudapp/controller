package app.simplecloud.controller.shared.host

import build.buf.gen.simplecloud.controller.v1.ServerHostDefinition
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ServerHost(
    val id: String,
    val host: String,
    val port: Int
) {

    fun toDefinition(): ServerHostDefinition {
        return ServerHostDefinition.newBuilder()
            .setHost(host)
            .setPort(port)
            .setUniqueId(id)
            .build()
    }

    companion object {
        @JvmStatic
        fun fromDefinition(serverHostDefinition: ServerHostDefinition): ServerHost {
            return ServerHost(
                serverHostDefinition.uniqueId,
                serverHostDefinition.host,
                serverHostDefinition.port
            )
        }
    }


    fun createChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }

}