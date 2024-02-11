package app.simplecloud.controller.api.group

import app.simplecloud.controller.shared.group.Group
import java.util.concurrent.CompletableFuture

interface GroupApi {
    /**
     * @param name the name of the group.
     * @return a [CompletableFuture] with the [Group].
     */
    fun getGroupByName(name: String): CompletableFuture<Group>
}