package app.simplecloud.controller.runtime

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.group.GroupService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class ControllerRuntime {

    private val logger = LogManager.getLogger(ControllerRuntime::class.java)

    private val groupRepository = GroupRepository()

    private val server = createGrpcServerFromEnv()

    fun start() {
        logger.info("Starting controller...")
        startGrpcServer()
    }

    fun startGrpcServer() {
        logger.info("Starting gRPC server...")
        thread {
            server.start()
            server.awaitTermination()
        }
    }

    private fun createGrpcServerFromEnv(): Server {
        val port = System.getenv("GRPC_PORT")?.toInt() ?: 5816
        return ServerBuilder.forPort(port)
            .addService(
                GroupService(groupRepository),
            )
            .build()
    }

}