package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.shared.auth.AuthCallCredentials
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.time.ProtoBufTimestamp
import app.simplecloud.pubsub.PubSubClient
import build.buf.gen.simplecloud.controller.v1.*
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.coroutineScope
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.util.*

class ServerService(
    private val numericalIdRepository: ServerNumericalIdRepository,
    private val serverRepository: ServerRepository,
    private val hostRepository: ServerHostRepository,
    private val groupRepository: GroupRepository,
    private val forwardingSecret: String,
    private val authCallCredentials: AuthCallCredentials,
    private val pubSubClient: PubSubClient,
) : ControllerServerServiceGrpcKt.ControllerServerServiceCoroutineImplBase() {

    private val logger = LogManager.getLogger(ServerService::class.java)

    override suspend fun attachServerHost(request: AttachServerHostRequest): ServerHostDefinition {
        val serverHost = ServerHost.fromDefinition(request.serverHost)
        try {
            hostRepository.delete(serverHost)
            hostRepository.save(serverHost)
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Could not save serverhost").withCause(e))
        }
        logger.info("Successfully registered ServerHost ${serverHost.id}.")

        coroutineScope {
            val channel = serverHost.createChannel()
            val stub =
                ServerHostServiceGrpcKt.ServerHostServiceCoroutineStub(channel).withCallCredentials(authCallCredentials)
            serverRepository.findServersByHostId(serverHost.id).forEach { server ->
                logger.info("Reattaching Server ${server.uniqueId} of group ${server.group}...")
                try {
                    val result = stub.reattachServer(server.toDefinition())
                    serverRepository.save(Server.fromDefinition(result))
                    logger.info("Success!")
                } catch (e: Exception) {
                    logger.error("Server was found to be offline, unregistering...")
                    serverRepository.delete(server)
                }
            }
            channel.shutdown()
        }
        return serverHost.toDefinition()
    }

    override suspend fun getAllServers(request: GetAllServersRequest): GetAllServersResponse {
        val currentServers = serverRepository.getAll()
        return getAllServersResponse { servers.addAll(currentServers.map { it.toDefinition() }) }
    }

    override suspend fun getServerByNumerical(request: GetServerByNumericalRequest): ServerDefinition {
        val server = serverRepository.findServerByNumerical(request.groupName, request.numericalId.toInt())
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No server was found matching this group and numerical id"))
        return server.toDefinition()
    }

    override suspend fun stopServerByNumerical(request: StopServerByNumericalRequest): ServerDefinition {
        val server = serverRepository.findServerByNumerical(request.groupName, request.numericalId.toInt())
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No server was found matching this group and numerical id"))
        try {
            return stopServer(server.toDefinition())
        } catch (e: Exception) {
            throw StatusException(
                Status.INTERNAL.withDescription("Error occured whilest cleaning up stopped server: ").withCause(e)
            )
        }
    }

    override suspend fun updateServer(request: UpdateServerRequest): ServerDefinition {
        val deleted = request.deleted
        val server = Server.fromDefinition(request.server)
        if (!deleted) {
            try {
                val before = serverRepository.find(server.uniqueId)
                    ?: throw StatusException(Status.NOT_FOUND.withDescription("Server not found"))
                pubSubClient.publish(
                    "event",
                    ServerUpdateEvent.newBuilder()
                        .setUpdatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                        .setServerBefore(before.toDefinition()).setServerAfter(request.server).build()
                )
                serverRepository.save(server)
                return server.toDefinition()
            } catch (e: Exception) {
                throw StatusException(
                    Status.INTERNAL
                        .withDescription("Could not update server")
                        .withCause(e)
                )
            }
        } else {
            logger.info("Deleting server ${server.uniqueId} of group ${request.server.groupName}...")
            val deleteSuccess = serverRepository.delete(server)
            if (!deleteSuccess) {
                throw StatusException(
                    Status.INTERNAL
                        .withDescription("Could not delete server")
                )
            }
            logger.info("Deleted server ${server.uniqueId} of group ${request.server.groupName}.")
            pubSubClient.publish(
                "event", ServerStopEvent.newBuilder()
                    .setServer(request.server)
                    .setStoppedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                    .setStopCause(ServerStopCause.NATURAL_STOP)
                    .setTerminationMode(ServerTerminationMode.UNKNOWN_MODE) //TODO: Add proto fields to make changing this possible
                    .build()
            )
            return server.toDefinition()
        }
    }

    override suspend fun getServerById(request: GetServerByIdRequest): ServerDefinition {
        val server = serverRepository.find(request.serverId) ?: throw StatusException(
            Status.NOT_FOUND
                .withDescription("No server was found matching this unique id")
        )
        return server.toDefinition()
    }

    override suspend fun getServersByGroup(request: GetServersByGroupRequest): GetServersByGroupResponse {
        val groupServers = serverRepository.findServersByGroup(request.groupName)
        return getServersByGroupResponse { servers.addAll(groupServers.map { it.toDefinition() }) }
    }

    override suspend fun getServersByType(request: ServerTypeRequest): GetServersByTypeResponse {
        val typeServers = serverRepository.findServersByType(request.serverType)
        return getServersByTypeResponse { servers.addAll(typeServers.map { it.toDefinition() }) }
    }

    override suspend fun startServer(request: ControllerStartServerRequest): ServerDefinition {
        val host = hostRepository.find(serverRepository)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No server host found, could not start server"))
        val group = groupRepository.find(request.groupName)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No group was found matching this name"))
        try {
            val server = startServer(host, group)
            pubSubClient.publish(
                "event", ServerStartEvent.newBuilder()
                    .setServer(server)
                    .setStartedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                    .setStartCause(request.startCause)
                    .build()
            )
            return server
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Error whilst starting server").withCause(e))
        }
    }

    private suspend fun startServer(host: ServerHost, group: Group): ServerDefinition {
        val numericalId = numericalIdRepository.findNextNumericalId(group.name)
        val server = buildServer(group, numericalId, forwardingSecret)
        serverRepository.save(server)
        val channel = host.createChannel()
        val stub = ServerHostServiceGrpcKt.ServerHostServiceCoroutineStub(channel)
            .withCallCredentials(authCallCredentials)
        serverRepository.save(server)
        try {
            val result = stub.startServer(
                ServerHostStartServerRequest.newBuilder()
                    .setGroup(group.toDefinition())
                    .setServer(server.toDefinition())
                    .build()
            )
            serverRepository.save(Server.fromDefinition(result))
            channel.shutdown()
            return result
        } catch (e: Exception) {
            serverRepository.delete(server)
            numericalIdRepository.removeNumericalId(group.name, server.numericalId)
            channel.shutdown()
            logger.error("Error whilst starting server:", e)
            throw e
        }
    }

    private fun buildServer(group: Group, numericalId: Int, forwardingSecret: String): Server {
        return Server.fromDefinition(
            ServerDefinition.newBuilder()
                .setNumericalId(numericalId)
                .setServerType(group.type)
                .setGroupName(group.name)
                .setMinimumMemory(group.minMemory)
                .setMaximumMemory(group.maxMemory)
                .setServerState(ServerState.PREPARING)
                .setMaxPlayers(group.maxPlayers)
                .setCreatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setUpdatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setPlayerCount(0)
                .setUniqueId(UUID.randomUUID().toString().replace("-", "")).putAllCloudProperties(
                    mapOf(
                        *group.properties.entries.map { it.key to it.value }.toTypedArray(),
                        "forwarding-secret" to forwardingSecret,
                    )
                ).build()
        )
    }

    override suspend fun stopServer(request: StopServerRequest): ServerDefinition {
        val server = serverRepository.find(request.serverId)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("No server was found matching this id."))
        try {
            val stopped = stopServer(server.toDefinition())
            pubSubClient.publish(
                "event", ServerStopEvent.newBuilder()
                    .setServer(stopped)
                    .setStoppedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                    .setStopCause(request.stopCause)
                    .setTerminationMode(ServerTerminationMode.UNKNOWN_MODE) //TODO: Add proto fields to make changing this possible
                    .build()
            )
            return stopped
        } catch (e: Exception) {
            throw StatusException(Status.INTERNAL.withDescription("Error whilst stopping server").withCause(e))
        }
    }

    private suspend fun stopServer(server: ServerDefinition): ServerDefinition {
        val host = hostRepository.findServerHostById(server.hostId)
            ?: throw Status.NOT_FOUND
                .withDescription("No server host was found matching this server.")
                .asRuntimeException()
        val channel = host.createChannel()
        val stub = ServerHostServiceGrpcKt.ServerHostServiceCoroutineStub(channel)
            .withCallCredentials(authCallCredentials)
        try {
            val stopped = stub.stopServer(server)
            channel.shutdown()
            return stopped
        } catch (e: Exception) {
            logger.error("Server stop error occured:", e)
            throw e
        }
    }

    override suspend fun updateServerProperty(request: UpdateServerPropertyRequest): ServerDefinition {
        val server = serverRepository.find(request.serverId)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Server with id ${request.serverId} does not exist."))
        val serverBefore = server.copy()
        server.properties[request.propertyKey] = request.propertyValue
        serverRepository.save(server)
        pubSubClient.publish(
            "event",
            ServerUpdateEvent.newBuilder().setUpdatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setServerBefore(serverBefore.toDefinition()).setServerAfter(server.toDefinition()).build()
        )
        return server.toDefinition()
    }

    override suspend fun updateServerState(request: UpdateServerStateRequest): ServerDefinition {
        val server = serverRepository.find(request.serverId)
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Server with id ${request.serverId} does not exist."))
        val serverBefore = server.copy()
        server.state = request.serverState
        serverRepository.save(server)
        pubSubClient.publish(
            "event",
            ServerUpdateEvent.newBuilder().setUpdatedAt(ProtoBufTimestamp.fromLocalDateTime(LocalDateTime.now()))
                .setServerBefore(serverBefore.toDefinition()).setServerAfter(server.toDefinition()).build()
        )
        return server.toDefinition()
    }

}