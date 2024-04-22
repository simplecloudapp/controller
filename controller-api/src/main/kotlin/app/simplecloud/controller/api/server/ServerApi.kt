package app.simplecloud.controller.api.server

import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.ServerType
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.controller.shared.status.ApiResponse
import build.buf.gen.simplecloud.controller.v1.ServerState
import java.util.concurrent.CompletableFuture

interface ServerApi {

    /**
     * @return a [CompletableFuture] with a [List] of all [Server]s
     */
    fun getAllServers(): CompletableFuture<List<Server>>

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
     * @param type The servers type
     * @return a [CompletableFuture] with a [List] of all [Server]s with this type
     */
    fun getServersByType(type: ServerType): CompletableFuture<List<Server>>

    /**
     * @param groupName the group name of the group the new server should be of.
     * @return a [CompletableFuture] with a [Server] or null.
     */
    fun startServer(groupName: String): CompletableFuture<Server?>

    /**
     * @param groupName the group name of the servers group.
     * @param numericalId the numerical id of the server.
     * @return a [CompletableFuture] with a [ApiResponse].
     */
    fun stopServer(groupName: String, numericalId: Long): CompletableFuture<ApiResponse>

    /**
     * @param id the id of the server.
     * @return a [CompletableFuture] with a [ApiResponse].
     */
    fun stopServer(id: String): CompletableFuture<ApiResponse>

    /**
     * @param id the id of the server.
     * @param state the new state of the server.
     * @return a [CompletableFuture] with a [ApiResponse].
     */
    fun updateServerState(id: String, state: ServerState): CompletableFuture<ApiResponse>

    /**
     * @param id the id of the server.
     * @param key the server property key
     * @param value the new property value
     * @return a [CompletableFuture] with a [ApiResponse].
     */
    fun updateServerProperty(id: String, key: String, value: Any): CompletableFuture<ApiResponse>

}