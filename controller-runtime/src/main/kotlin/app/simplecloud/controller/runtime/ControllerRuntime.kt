package app.simplecloud.controller.runtime

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.group.GroupService
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.runtime.server.ServerService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class ControllerRuntime {

    private val logger = LogManager.getLogger(ControllerRuntime::class.java)

    private val groupRepository = GroupRepository()
    private val serverRepository = ServerRepository()
    private val hostRepository = ServerHostRepository()

    private val server = createGrpcServerFromEnv()

    fun start() {
        logger.info("Starting controller...")
        startGrpcServer()
    }

    private fun startGrpcServer() {
        logger.info("Starting gRPC server...")
        thread {
            server.start()
            server.awaitTermination()
        }
    }

    private fun createGrpcServerFromEnv(): Server {
        val port = System.getenv("GRPC_PORT")?.toInt() ?: 5816
        return ServerBuilder.forPort(port)
                .addService(GroupService(groupRepository))
                .addService(ServerService(serverRepository, hostRepository, groupRepository))
                .build()
    }

}