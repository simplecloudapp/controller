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
import java.util.concurrent.ConcurrentHashMap

class Reconciler(
  private val groupRepository: GroupRepository,
  private val serverRepository: ServerRepository,
  private val serverHostRepository: ServerHostRepository,
  managedChannel: ManagedChannel,
) {

  private val serverStub = ControllerServerServiceGrpc.newFutureStub(managedChannel)
  private val logger = LogManager.getLogger(Reconciler::class.java)

  private val startingGroupNames = ConcurrentHashMap<String, Int>()

  fun reconcile() {
    groupRepository.forEach { group ->
      val servers = serverRepository.findServersByGroup(group.name)
      val availableServerCount = servers.count { server ->
        server.state == ServerState.AVAILABLE || server.state == ServerState.STARTING
      }
      val startingServers = startingGroupNames.getOrDefault(group.name, 0)

      cleanupServers()
      startServers(group, availableServerCount, startingServers, servers.size)
    }
  }

  private fun cleanupServers() {

  }

  private fun startServers(
    group: Group,
    availableServerCount: Int,
    startingServers: Int,
    serverCount: Int
  ) {
    if (!checkIfNewServerCanBeStarted(group, availableServerCount, startingServers, serverCount) ||
      !serverHostRepository.areServerHostsAvailable()
    ) {
      return
    }

    startingGroupNames[group.name] = startingGroupNames.getOrDefault(group.name, 0) + 1
    startServer(group)
  }

  private fun startServer(group: Group) {
    logger.info("Starting new instance of group ${group.name}")
    serverStub.startServer(GroupNameRequest.newBuilder().setName(group.name).build()).toCompletable()
      .thenApply {
        cleanupStartingGroup(group)
        logger.info("Started new instance ${it.groupName}-${it.numericalId}/${it.uniqueId} of group ${group.name} on ${it.ip}:${it.port}")
      }.exceptionally {
        cleanupStartingGroup(group)
        logger.error("Could not start a new instance of group ${group.name}: ${it.message}")
      }
  }

  @Synchronized
  private fun cleanupStartingGroup(group: Group) {
    if (startingGroupNames.getOrDefault(group.name, 0) <= 1) {
      startingGroupNames.remove(group.name)
      return
    }

    startingGroupNames[group.name] = startingGroupNames[group.name]!! - 1
  }

  private fun checkIfNewServerCanBeStarted(
    group: Group,
    availableServerCount: Int,
    startingServers: Int,
    serverCount: Int
  ): Boolean {
    return getNeededServerCount(group, availableServerCount, startingServers, serverCount) > 0
  }

  private fun getNeededServerCount(
    group: Group,
    availableServerCount: Int,
    startingServers: Int,
    serverCount: Int
  ): Int {
    if (!checkIfServersAreNeeded(group, availableServerCount, serverCount)) {
      return 0
    }

    return (group.minOnlineCount - availableServerCount - startingServers).toInt()
  }

  private fun checkIfServersAreNeeded(group: Group, availableServerCount: Int, serverCount: Int): Boolean {
    return availableServerCount < group.minOnlineCount && serverCount < group.maxOnlineCount
  }

}