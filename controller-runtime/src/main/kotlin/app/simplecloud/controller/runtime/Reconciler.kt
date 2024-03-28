package app.simplecloud.controller.runtime

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.ControllerServerServiceGrpc
import app.simplecloud.controller.shared.proto.GroupNameRequest
import app.simplecloud.controller.shared.proto.ServerState
import io.grpc.ManagedChannel
import org.apache.logging.log4j.LogManager

class Reconciler(
  private val groupRepository: GroupRepository,
  private val serverRepository: ServerRepository,
  private val serverHostRepository: ServerHostRepository,
  managedChannel: ManagedChannel,
) {

  private val serverStub = ControllerServerServiceGrpc.newFutureStub(managedChannel)
  private val logger = LogManager.getLogger(Reconciler::class.java)

  fun reconcile() {
    groupRepository.forEach { group ->
      val servers = serverRepository.findServersByGroup(group.name)
      val availableServerCount = servers.count { server ->
        server.state == ServerState.AVAILABLE
          || server.state == ServerState.STARTING
          || server.state == ServerState.PREPARING
      }

      logger.info("Reconciling group ${group.name} with ${servers.size} servers, $availableServerCount available servers")

      cleanupServers()
      startServers(group, availableServerCount, servers.size)
    }
  }

  private fun cleanupServers() {

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