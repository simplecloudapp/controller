package app.simplecloud.controller.api.server

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.ServerType
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.status.ApiResponse
import java.util.concurrent.CompletableFuture

interface ServerApi {

    /**
     * @param id the id of the server.
     * @return a [CompletableFuture] with the [Server].
     */
    fun getServerById(id: String): CompletableFuture<Server>

    /**
     * @param groupName the name of the server group.
     * @return a [CompletableFuture] with a [List] of [Server]s of that group.
     */
    fun getServersByGroup(groupName: String): CompletableFuture<List<Server>>

    /**
     * @param group The server group.
     * @return a [CompletableFuture] with a [List] of [Server]s of that group.
     */
    fun getServersByGroup(group: Group): CompletableFuture<List<Server>>

    /**
     * @return a [CompletableFuture] with a [List] of all [Server]s
     */
    fun getServersByType(type: ServerType): CompletableFuture<List<Server>>

    /**
     * @param groupName the group name of the group the new server should be of.
     * @return a [CompletableFuture] with a [Server] or null.
     */
    fun startServer(groupName: String): CompletableFuture<Server?>

    /**
     * @param id the id of the server.
     * @return a [CompletableFuture] with a [ApiResponse].
     */
    fun stopServer(id: String): CompletableFuture<ApiResponse>
}