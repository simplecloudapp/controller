package app.simplecloud.controller.api.group

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.status.ApiResponse
import java.util.concurrent.CompletableFuture

interface GroupApi {
    /**
     * @param name the name of the group.
     * @return a [CompletableFuture] with the [Group].
     */
    fun getGroupByName(name: String): CompletableFuture<Group>

    /**
     * @param name the name of the group.
     * @return a status [ApiResponse] of the delete state.
     */
    fun deleteGroup(name: String): CompletableFuture<ApiResponse>

    /**
     * @param group the [Group] to create.
     * @return a status [ApiResponse] of the creation state.
     */
    fun createGroup(group: Group): CompletableFuture<ApiResponse>
}