package app.simplecloud.controller.shared.host

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

data class ServerHost(
        val id: String,
        val host: String,
        val port: Int
) {

    @Transient
    val endpoint: ManagedChannel = createChannel()

    private fun createChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }

}