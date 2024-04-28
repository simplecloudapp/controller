package app.simplecloud.controller.api.impl

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.api.GroupApi
import app.simplecloud.controller.api.ServerApi
import app.simplecloud.controller.shared.auth.AuthCallCredentials
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class ControllerApiImpl(
    authSecret: String
): ControllerApi {

    private val authCallCredentials = AuthCallCredentials(authSecret)

    private val managedChannel = createManagedChannelFromEnv()
    private val groups: GroupApi = GroupApiImpl(managedChannel, authCallCredentials)
    private val servers: ServerApi = ServerApiImpl(managedChannel, authCallCredentials)

    override fun getGroups(): GroupApi {
        return groups
    }

    override fun getServers(): ServerApi {
        return servers
    }

    private fun createManagedChannelFromEnv(): ManagedChannel {
        val host = System.getenv("CONTROLLER_HOST") ?: "127.0.0.1"
        val port = System.getenv("CONTROLLER_PORT")?.toInt() ?: 5816
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }

}