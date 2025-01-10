package app.simplecloud.controller.runtime.reconciler.type

import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.ServerState

/**
 * @author Niklas Nieberler
 */

class PlayerRatioReconcilerHandler(
    private val playerRatio: Int
) : ReconcilerTypeHandler {

    override suspend fun reconcile(serverRepository: ServerRepository, group: Group): Long {
        val availableServers = serverRepository.findServersByGroup(group.name)
            .filter { it.state != ServerState.INGAME }
        val serversWithHighPlayerRatio = availableServers
            .filter { (it.playerCount / group.maxPlayers) * 100 >= this.playerRatio }
        val serverCountWithLowPlayerRatio = availableServers.size - serversWithHighPlayerRatio.size
        if (serverCountWithLowPlayerRatio < group.minOnlineCount) {
            return availableServers.size + (group.minOnlineCount - serverCountWithLowPlayerRatio)
        }
        return availableServers.size.toLong()
    }

}