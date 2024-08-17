package app.simplecloud.controller.runtime

import app.simplecloud.controller.runtime.database.DatabaseFactory
import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.group.GroupService
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.launcher.ControllerStartCommand
import app.simplecloud.controller.runtime.reconciler.Reconciler
import app.simplecloud.controller.runtime.server.ServerNumericalIdRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.runtime.server.ServerService
import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.auth.AuthSecretInterceptor
import app.simplecloud.pubsub.PubSubService
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

class ControllerRuntime(
    private val controllerStartCommand: ControllerStartCommand
) {

    private val logger = LogManager.getLogger(ControllerRuntime::class.java)
    private val database = DatabaseFactory.createDatabase(controllerStartCommand.databaseUrl)
    private val authCallCredentials = AuthCallCredentials(controllerStartCommand.authSecret)

    private val groupRepository = GroupRepository(controllerStartCommand.groupPath)
    private val numericalIdRepository = ServerNumericalIdRepository()
    private val serverRepository = ServerRepository(database, numericalIdRepository)
    private val hostRepository = ServerHostRepository()
    private val reconciler = Reconciler(
        groupRepository,
        serverRepository,
        hostRepository,
        numericalIdRepository,
        createManagedChannel(),
        authCallCredentials
    )
    private val server = createGrpcServer()
    private val pubSubServer = createPubSubGrpcServer()

    fun start() {
        setupDatabase()
        startGrpcServer()
        startPubSubGrpcServer()
        startReconciler()
        loadGroups()
        loadServers()
    }

    private fun setupDatabase() {
        logger.info("Setting up database...")
        database.setup()
    }

    private fun loadServers() {
        logger.info("Loading servers...")
        val loadedServers = serverRepository.load()
        if (loadedServers.isEmpty()) {
            return
        }

        logger.info("Loaded servers from cache: ${loadedServers.joinToString { "${it.group}-${it.numericalId}" }}")
    }

    private fun startGrpcServer() {
        logger.info("Starting gRPC server...")
        thread {
            server.start()
            server.awaitTermination()
        }
    }

    private fun startPubSubGrpcServer() {
        logger.info("Starting pubsub gRPC server...")
        thread {
            pubSubServer.start()
            pubSubServer.awaitTermination()
        }
    }

    private fun startReconciler() {
        logger.info("Starting Reconciler...")
        startReconcilerJob()
    }

    private fun loadGroups() {
        logger.info("Loading groups...")
        val loadedGroups = groupRepository.load()
        if (loadedGroups.isEmpty()) {
            logger.warn("No groups found.")
            return
        }

        logger.info("Loaded groups: ${loadedGroups.joinToString { it.name }}")
    }

    private fun createGrpcServer(): Server {
        return ServerBuilder.forPort(controllerStartCommand.grpcPort)
            .addService(GroupService(groupRepository))
            .addService(
                ServerService(
                    numericalIdRepository,
                    serverRepository,
                    hostRepository,
                    groupRepository,
                    controllerStartCommand.forwardingSecret,
                    authCallCredentials
                )
            )
            .intercept(AuthSecretInterceptor(controllerStartCommand.authSecret))
            .build()
    }

    private fun createPubSubGrpcServer(): Server {
        return ServerBuilder.forPort(controllerStartCommand.pubSubGrpcPort)
            .addService(PubSubService())
            .intercept(AuthSecretInterceptor(controllerStartCommand.authSecret))
            .build()
    }

    private fun createManagedChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(controllerStartCommand.grpcHost, controllerStartCommand.grpcPort)
            .usePlaintext()
            .build()
    }

    private fun startReconcilerJob(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                reconciler.reconcile()
                delay(2000L)
            }
        }
    }

}