package app.simplecloud.controller.runtime

import app.simplecloud.controller.shared.db.DatabaseConfig
import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.group.GroupService
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.server.ServerNumericalIdRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.runtime.server.ServerService
import app.simplecloud.controller.shared.db.Database
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import kotlin.concurrent.thread

class ControllerRuntime {

    private val logger = LogManager.getLogger(ControllerRuntime::class.java)

    private lateinit var groupRepository: GroupRepository
    private val numericalIdRepository = ServerNumericalIdRepository()
    private lateinit var serverRepository: ServerRepository
    private val hostRepository: ServerHostRepository = ServerHostRepository()
    private lateinit var databaseConfig: DatabaseConfig
    private lateinit var reconciler: Reconciler
    private lateinit var server: Server

    fun start(args: MutableMap<String, String>) {
        groupRepository = GroupRepository(Path.of(args.getOrDefault("groups-path", "groups")))
        groupRepository.loadAll()
        logger.info("Starting database...")
        loadDB()
        logger.info("Starting controller...")
        server = createGrpcServerFromEnv()
        startGrpcServer()
        startReconciler()
    }


    private fun loadDB() {
        logger.info("Loading database configuration...")
        databaseConfig = DatabaseConfig.load("/database-config.yml")!!
        logger.info("Connecting database...")
        Database.init(databaseConfig)
        serverRepository = ServerRepository(numericalIdRepository)
        serverRepository.load()
    }

    private fun startGrpcServer() {
        logger.info("Starting gRPC server...")
        thread {
            server.start()
            server.awaitTermination()
        }
    }

    private fun startReconciler() {
        logger.info("Starting Reconciler...")
        reconciler = Reconciler(groupRepository, serverRepository, hostRepository, createManagedChannel())
        startReconcilerJob()
    }

    private fun createGrpcServerFromEnv(): Server {
        val port = System.getenv("GRPC_PORT")?.toInt() ?: 5816
        return ServerBuilder.forPort(port)
            .addService(GroupService(groupRepository))
            .addService(ServerService(numericalIdRepository, serverRepository, hostRepository, groupRepository))
            .build()
    }

    private fun createManagedChannel(): ManagedChannel {
        val port = System.getenv("GRPC_PORT")?.toInt() ?: 5816
        return ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun startReconcilerJob(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (NonCancellable.isActive) {
                reconciler.reconcile()
                delay(2000L)
            }
        }
    }

}