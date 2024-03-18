package app.simplecloud.controller.runtime

import app.simplecloud.controller.runtime.group.GroupRepository
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.future.toCompletable
import app.simplecloud.controller.shared.proto.ControllerServerServiceGrpc
import app.simplecloud.controller.shared.proto.GroupNameRequest
import io.grpc.ManagedChannel
import org.apache.logging.log4j.LogManager

class Reconciler(
    private val groupRepository: GroupRepository,
    private val serverRepository: ServerRepository,
    managedChannel: ManagedChannel,
) {

    private val serverStub = ControllerServerServiceGrpc.newFutureStub(managedChannel)
    private val logger = LogManager.getLogger(Reconciler::class.java)
    fun reconcile() {
        groupRepository.forEach { group ->
            val servers = serverRepository.findServersByGroup(group.name)
            val full = servers.filter { server ->
                server.playerCount >= group.maxOnlineCount
            }
            if (servers.size < group.minOnlineCount || (full.size >= servers.size && servers.size < group.maxOnlineCount)) {
                serverStub.startServer(GroupNameRequest.newBuilder().setName(group.name).build()).toCompletable()
                    .thenApply {
                        logger.info("Started new instance ${it.uniqueId} of group ${group.name} on ${it.ip}:${it.port}")
                    }.exceptionally {
                    logger.error("Could not start a new instance of group ${group.name}: ${it.message}")
                }
            }
        }
    }

}