package app.simplecloud.controller.runtime.reconciler

import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.server.ServerNumericalIdRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ControllerServerServiceGrpc.ControllerServerServiceFutureStub
import build.buf.gen.simplecloud.controller.v1.GroupNameRequest
import build.buf.gen.simplecloud.controller.v1.ServerIdRequest
import build.buf.gen.simplecloud.controller.v1.ServerState
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import kotlin.math.min

/**
 * Date: 20.04.24
 * Time: 16:37
 * @author Frederick Baier
 *
 */
class GroupReconciler(
    private val serverRepository: ServerRepository,
    private val serverHostRepository: ServerHostRepository,
    private val numericalIdRepository: ServerNumericalIdRepository,
    private val serverStub: ControllerServerServiceFutureStub,
    private val group: Group,
) {

    private val logger = LogManager.getLogger(Reconciler::class.java)
    private val servers = this.serverRepository.findServersByGroup(this.group.name)

    private val availableServerCount = calculateAvailableServerCount()

    fun reconcile() {
        cleanupServers()
        cleanupNumericalIds()
        startServers()
    }

    private fun calculateAvailableServerCount(): Int {
        return servers.count { server ->
            server.state == ServerState.AVAILABLE
                    || server.state == ServerState.STARTING
                    || server.state == ServerState.PREPARING
        }
    }

    private fun cleanupServers() {
        val fullyStartedServers = this.servers.filter { it.state == ServerState.AVAILABLE }
        val hasMoreServersThenNeeded = fullyStartedServers.size > this.group.minOnlineCount
        if (!hasMoreServersThenNeeded)
            return

        val serverCountToStop = fullyStartedServers.size - this.group.minOnlineCount
        fullyStartedServers
            .filter { !wasUpdatedRecently(it) }
            .shuffled()
            .take(serverCountToStop.toInt())
            .forEach { stopServer(it) }
    }

    private fun stopServer(server: Server) {
        logger.info("Stopping server ${server.uniqueId} of group ${server.group}")
        serverStub.stopServer(
            ServerIdRequest.newBuilder()
                .setId(server.uniqueId)
                .build()
        ).toCompletable()
            .thenApply {
                logger.info("Stopped server ${server.uniqueId} of group ${server.group}")
            }.exceptionally {
                logger.error("Could not stop server ${server.uniqueId} of group ${server.group}: ${it.message}")
            }
    }

    private fun cleanupNumericalIds() {
        val usedNumericalIds = this.servers.map { it.numericalId }
        val numericalIds = this.numericalIdRepository.findNumericalIds(this.group.name)

        val unusedNumericalIds = numericalIds.filter { !usedNumericalIds.contains(it) }

        unusedNumericalIds.forEach { this.numericalIdRepository.removeNumericalId(this.group.name, it) }

        if (unusedNumericalIds.isNotEmpty()) {
            logger.info("Removed unused numerical ids $unusedNumericalIds of group ${this.group.name}")
        }
    }

    private fun wasUpdatedRecently(server: Server): Boolean {
        return server.updatedAt.isAfter(LocalDateTime.now().minusMinutes(INACTIVE_SERVER_TIME))
    }

    private fun startServers() {
        if (!serverHostRepository.areServerHostsAvailable())
            return

        if (isNewServerNeeded())
            startServer()
    }

    private fun startServer() {
        logger.info("Starting new instance of group ${this.group.name}")
        serverStub.startServer(GroupNameRequest.newBuilder().setName(this.group.name).build()).toCompletable()
            .thenApply {
                logger.info("Started new instance ${it.groupName}-${it.numericalId}/${it.uniqueId} of group ${this.group.name} on ${it.ip}:${it.port}")
            }.exceptionally {
                it.printStackTrace()
                logger.error("Could not start a new instance of group ${this.group.name}: ${it.message}")
            }
    }

    private fun isNewServerNeeded(): Boolean {
        return calculateServerCountToStart() > 0
    }

    private fun calculateServerCountToStart(): Int {
        val currentServerCount = this.servers.size
        val neededServerCount = this.group.minOnlineCount - this.availableServerCount
        val maxNewServers = this.group.maxOnlineCount - currentServerCount

        if (neededServerCount > 0)
            return min(neededServerCount, maxNewServers).toInt()
        return 0
    }

    companion object {
        private const val INACTIVE_SERVER_TIME = 5L
    }

}