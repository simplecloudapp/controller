package app.simplecloud.controller.shared.host

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class ServerHost {

    private var id: String;
    private var host: String;
    private var port: Int;
    private var endpoint: ManagedChannel

    constructor(id: String, host: String, port: Int) {
        this.id = id
        this.host = host
        this.port = port
        this.endpoint = createChannel()
    }

    fun getEndpoint(): ManagedChannel {
        return endpoint
    }

    fun getId(): String {
        return id;
    }

    fun getHost(): String {
        return host;
    }

    fun getPort(): Int {
        return port
    }

    private fun createChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }

}