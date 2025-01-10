package app.simplecloud.controller.runtime.reconciler

import app.simplecloud.controller.runtime.reconciler.config.ReconcilerConfig
import app.simplecloud.controller.runtime.reconciler.type.PlayerRatioReconcilerHandler
import app.simplecloud.controller.runtime.server.ServerRepository

/**
 * @author Niklas Nieberler
 */

class GroupReconciler(
    private val serverRepository: ServerRepository,
    private val config: ReconcilerConfig
) {

    private val typeHandlers = arrayOf(
        PlayerRatioReconcilerHandler(config.playerRatio ?: 0)
    )

    fun reconcile() {

    }

}