package app.simplecloud.controller.runtime

import app.simplecloud.controller.shared.db.DatabaseConfig
import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.group.GroupService
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.runtime.server.ServerService
import app.simplecloud.controller.shared.db.Database
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.concurrent.thread

class ControllerRuntime {

    private val logger = LogManager.getLogger(ControllerRuntime::class.java)

    private val groupRepository: GroupRepository = GroupRepository("/groups.yml")
    private var serverRepository: ServerRepository? = null
    private val hostRepository: ServerHostRepository = ServerHostRepository("/hosts.yml")
    private var databaseConfig: DatabaseConfig? = null

    private var server: Server? = null

    fun start() {
        logger.info("Starting database...")
        loadDB()
        logger.info("Starting controller...")
        server = createGrpcServerFromEnv()
        startGrpcServer()
    }


    private fun loadDB() {
        logger.info("Loading database configuration...")
        databaseConfig = DatabaseConfig.load("/database-config.yml")
        logger.info("Connecting database...")
        Database.init(databaseConfig!!)
        serverRepository = ServerRepository()
    }

    private fun startGrpcServer() {
        logger.info("Starting gRPC server...")
        thread {
            server!!.start()
            server!!.awaitTermination()
        }
    }

    private fun createGrpcServerFromEnv(): Server {
        val port = System.getenv("GRPC_PORT")?.toInt() ?: 5816
        return ServerBuilder.forPort(port)
                .addService(GroupService(groupRepository))
                .addService(ServerService(serverRepository!!, hostRepository, groupRepository))
                .build()
    }

}