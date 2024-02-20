package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.shared.db.Database
import app.simplecloud.controller.shared.db.Tables.CLOUD_SERVERS
import app.simplecloud.controller.shared.proto.ServerDefinition
import app.simplecloud.controller.shared.server.Server
import java.util.concurrent.CompletableFuture

class ServerRepository : Repository<Server>() {

    private val db = Database.get()

    fun findServerById(id: String): ServerDefinition? {
        return firstOrNull { it.id == id }?.toDefinition()
    }

    fun findServersByHostId(id: String): List<ServerDefinition> {
        return filter { it.host == id }.map { it.toDefinition() }
    }

    fun findServersByGroup(group: String): List<ServerDefinition> {
        return filter { server -> server.group == group }.map { server -> server.toDefinition() }
    }

    override fun load() {
        clear()
        val query = db.select().from(CLOUD_SERVERS).fetchInto(CLOUD_SERVERS)
        addAll(query.map { Server(it.uniqueId, it.hostId, it.groupName) })
    }

    override fun delete(element: Server): CompletableFuture<Boolean> {
        val server = firstOrNull { it.id == element.id }
        if (server == null) return CompletableFuture.completedFuture(false)
        return db.deleteFrom(CLOUD_SERVERS).where(CLOUD_SERVERS.UNIQUE_ID.eq(server.id)).executeAsync().toCompletableFuture().thenApply {
            return@thenApply it > 0 && remove(server)
        }
    }

    override fun save(element: Server) {
        val server = firstOrNull { it.id == element.id }
        if (server != null) {
            val index = indexOf(server)
            removeAt(index)
            add(index, element)
        } else {
            add(element)
        }
        db.insertInto(
                CLOUD_SERVERS,

                CLOUD_SERVERS.UNIQUE_ID,
                CLOUD_SERVERS.HOST_ID,
                CLOUD_SERVERS.GROUP_NAME,
        ).values(
                element.id,
                element.host,
                element.group,
        ).onDuplicateKeyUpdate()
    }

}