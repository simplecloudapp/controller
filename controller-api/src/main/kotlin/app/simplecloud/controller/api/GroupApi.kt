package app.simplecloud.controller.api

import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.ServerType
import java.util.concurrent.CompletableFuture

interface GroupApi {

    /**
     * @param name the name of the group.
     * @return a [CompletableFuture] with the [Group].
     */
    fun getGroupByName(name: String): CompletableFuture<Group>

    /**
     * @param name the name of the group.
     * @return the deleted [Group].
     */
    fun deleteGroup(name: String): CompletableFuture<Group>

    /**
     * @param group the [Group] to create.
     * @return the created [Group].
     */
    fun createGroup(group: Group): CompletableFuture<Group>

    /**
     * @param group the [Group] to update.
     * @return the updated [Group].
     */
    fun updateGroup(group: Group): CompletableFuture<Group>
    /**
     * @return a [CompletableFuture] with a list of all groups.
     */
    fun getAllGroups(): CompletableFuture<List<Group>>

    /**
     * @param type the [ServerType] of the group
     * @return a [CompletableFuture] with a list of all groups matching this type.
     */
    fun getGroupsByType(type: ServerType): CompletableFuture<List<Group>>

}