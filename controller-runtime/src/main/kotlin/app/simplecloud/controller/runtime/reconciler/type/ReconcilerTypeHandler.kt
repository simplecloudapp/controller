package app.simplecloud.controller.runtime.reconciler.type

import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.group.Group

/**
 * @author Niklas Nieberler
 */

interface ReconcilerTypeHandler {

    suspend fun reconcile(serverRepository: ServerRepository, group: Group): Long

}