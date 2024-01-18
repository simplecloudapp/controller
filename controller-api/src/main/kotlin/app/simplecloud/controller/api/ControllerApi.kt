package app.simplecloud.controller.api

import app.simplecloud.controller.shared.group.Group
import java.util.concurrent.CompletableFuture

interface ControllerApi {

    /**
     * NOTE: This may be moved to a separate api file.
     * @param name the name of the group.
     * @returns a [CompletableFuture] with the [Group].
     */
    fun getGroupByName(name: String): CompletableFuture<Group>

}