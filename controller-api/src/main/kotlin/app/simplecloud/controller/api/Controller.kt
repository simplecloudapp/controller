package app.simplecloud.controller.api

import app.simplecloud.controller.api.group.GroupApi
import app.simplecloud.controller.api.group.impl.GroupApiImpl
import app.simplecloud.controller.api.server.ServerApi
import app.simplecloud.controller.api.server.impl.ServerApiImpl
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class Controller {
    companion object {
        @JvmStatic
        lateinit var groupApi: GroupApi
            private set

        @JvmStatic
        lateinit var serverApi: ServerApi
            private set

        fun init(){
            initGroupApi(GroupApiImpl())
            initServerApi(ServerApiImpl())
        }

        private fun initGroupApi(groupApi: GroupApi) {
            this.groupApi = groupApi
        }

        private fun initServerApi(serverApi: ServerApi) {
            this.serverApi = serverApi
        }

        fun createManagedChannelFromEnv(): ManagedChannel {
            val host = System.getenv("CONTROLLER_HOST") ?: "127.0.0.1"
            val port = System.getenv("CONTROLLER_PORT")?.toInt() ?: 5816
            return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
        }
    }
}