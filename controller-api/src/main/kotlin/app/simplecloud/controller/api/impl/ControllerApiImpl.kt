package app.simplecloud.controller.api.impl

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.api.GroupApi
import app.simplecloud.controller.api.ServerApi
import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.pubsub.PubSubClient
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class ControllerApiImpl(
    authSecret: String
): ControllerApi {

    private val authCallCredentials = AuthCallCredentials(authSecret)

    private val managedChannel = createManagedChannelFromEnv()
    private val groups: GroupApi = GroupApiImpl(managedChannel, authCallCredentials)
    private val servers: ServerApi = ServerApiImpl(managedChannel, authCallCredentials)


    private val pubSubClient = PubSubClient(
        System.getenv("CONTROLLER_PUBSUB_HOST") ?: "localhost",
        System.getenv("CONTROLLER_PUBSUB_PORT")?.toInt() ?: 5817,
        authCallCredentials,
    )

    /**
     * @return The controllers [GroupApi]
     */
    override fun getGroups(): GroupApi {
        return groups
    }

    /**
     * @return The controllers [ServerApi]
     */
    override fun getServers(): ServerApi {
        return servers
    }

    /**
     * @return The [PubSubClient] to subscribe to Controller events and send messages
     */
    override fun getPubSubClient(): PubSubClient {
        return pubSubClient
    }

    private fun createManagedChannelFromEnv(): ManagedChannel {
        val host = System.getenv("CONTROLLER_HOST") ?: "127.0.0.1"
        val port = System.getenv("CONTROLLER_PORT")?.toInt() ?: 5816
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }


}