package app.simplecloud.controller.runtime

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.ControllerServerServiceGrpc
import app.simplecloud.controller.shared.proto.GroupNameRequest
import app.simplecloud.controller.shared.proto.ServerIdRequest
import app.simplecloud.controller.shared.proto.ServerState
import app.simplecloud.controller.shared.server.Server
import io.grpc.ManagedChannel
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime

class Reconciler(
    private val groupRepository: GroupRepository,
    private val serverRepository: ServerRepository,
    private val serverHostRepository: ServerHostRepository,
    managedChannel: ManagedChannel,
) {

    private val INACTIVE_SERVER_TIME = 5L

    private val serverStub = ControllerServerServiceGrpc.newFutureStub(managedChannel)
    private val logger = LogManager.getLogger(Reconciler::class.java)

    fun reconcile() {
        groupRepository.findAll().forEach { group ->
            val servers = serverRepository.findServersByGroup(group.name)
            val availableServerCount = servers.count { server ->
                server.state == ServerState.AVAILABLE
                        || server.state == ServerState.STARTING
                        || server.state == ServerState.PREPARING
            }

            cleanupServers(group, servers, availableServerCount)
            startServers(group, availableServerCount, servers.size)
        }
    }

    private fun cleanupServers(group: Group, servers: List<Server>, availableServerCount: Int) {
        val hasMoreServersThenNeeded = availableServerCount > group.minOnlineCount
        servers
            .filter { it.state == ServerState.AVAILABLE }
            .forEach { server ->
                if (hasMoreServersThenNeeded && !wasUpdatedRecently(server)) {
                    logger.info("Stopping server ${server.uniqueId} of group ${group.name}")
                    serverStub.stopServer(
                        ServerIdRequest.newBuilder()
                            .setId(server.uniqueId)
                            .build()
                    ).toCompletable()
                        .thenApply {
                            logger.info("Stopped server ${server.uniqueId} of group ${group.name}")
                        }.exceptionally {
                            logger.error("Could not stop server ${server.uniqueId} of group ${group.name}: ${it.message}")
                        }
                }
            }
    }

    private fun wasUpdatedRecently(server: Server): Boolean {
        return server.updatedAt.isAfter(LocalDateTime.now().minusMinutes(INACTIVE_SERVER_TIME))
    }

    private fun startServers(
        group: Group,
        availableServerCount: Int,
        serverCount: Int
    ) {
        if (!checkIfNewServerCanBeStarted(group, availableServerCount, serverCount) ||
            !serverHostRepository.areServerHostsAvailable()
        ) {
            return
        }

        startServer(group)
    }

    private fun startServer(group: Group) {
        logger.info("Starting new instance of group ${group.name}")
        serverStub.startServer(GroupNameRequest.newBuilder().setName(group.name).build()).toCompletable()
            .thenApply {
                logger.info("Started new instance ${it.groupName}-${it.numericalId}/${it.uniqueId} of group ${group.name} on ${it.ip}:${it.port}")
            }.exceptionally {
                it.printStackTrace()
                logger.error("Could not start a new instance of group ${group.name}: ${it.message}")
            }
    }

    private fun checkIfNewServerCanBeStarted(
        group: Group,
        availableServerCount: Int,
        serverCount: Int
    ): Boolean {
        return getNeededServerCount(group, availableServerCount, serverCount) > 0
    }

    private fun getNeededServerCount(
        group: Group,
        availableServerCount: Int,
        serverCount: Int
    ): Int {
        if (!checkIfServersAreNeeded(group, availableServerCount, serverCount)) {
            return 0
        }

        return (group.minOnlineCount - availableServerCount).toInt()
    }

    private fun checkIfServersAreNeeded(group: Group, availableServerCount: Int, serverCount: Int): Boolean {
        return availableServerCount < group.minOnlineCount && serverCount < group.maxOnlineCount
    }

}