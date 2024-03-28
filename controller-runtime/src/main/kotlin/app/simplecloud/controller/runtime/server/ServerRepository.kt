package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.shared.db.Database
import app.simplecloud.controller.shared.db.Tables.CLOUD_SERVERS
import app.simplecloud.controller.shared.db.Tables.CLOUD_SERVER_PROPERTIES
import app.simplecloud.controller.shared.proto.ServerDefinition
import app.simplecloud.controller.shared.proto.ServerState
import app.simplecloud.controller.shared.server.Server
import java.util.concurrent.CompletableFuture

class ServerRepository : Repository<Server>() {

    private val db = Database.get()

    fun findServerById(id: String): ServerDefinition? {
        return firstOrNull { it.uniqueId == id }?.toDefinition()
    }

    fun findServersByHostId(id: String): List<ServerDefinition> {
        return filter { it.host == id }.map { it.toDefinition() }
    }

    fun findServersByGroup(group: String): List<ServerDefinition> {
        return filter { server -> server.group == group }.map { server -> server.toDefinition() }
    }

    fun findNextNumericalId(group: String): Int {
        var id = 1
        findServersByGroup(group).sortedWith(compareBy { it.numericalId }).forEach {
            if(it.numericalId == id) id++
        }
        return id
    }

    override fun load() {
        clear()
        val query = db.select().from(CLOUD_SERVERS).fetchInto(CLOUD_SERVERS)
        query.map {
            val propertiesQuery = db.select().from(CLOUD_SERVER_PROPERTIES).fetchInto(CLOUD_SERVER_PROPERTIES)
            add(
                Server(
                    it.uniqueId,
                    it.groupName,
                    it.hostId,
                    it.numericalId,
                    it.templateId,
                    it.ip,
                    it.port.toLong(),
                    it.minimumMemory.toLong(),
                    it.maximumMemory.toLong(),
                    it.playerCount.toLong(),
                    propertiesQuery.map { item ->
                        item.key to item.value
                    }.toMap().toMutableMap(),
                    ServerState.valueOf(it.state)
                )
            )
        }
    }

    override fun delete(element: Server): CompletableFuture<Boolean> {
        val server = firstOrNull { it.uniqueId == element.uniqueId }
        if (server == null) return CompletableFuture.completedFuture(false)
        val canDelete =
            db.deleteFrom(CLOUD_SERVER_PROPERTIES).where(CLOUD_SERVER_PROPERTIES.SERVER_ID.eq(server.uniqueId))
                .executeAsync().toCompletableFuture().thenApply {
                    return@thenApply true
                }.exceptionally {
                    it.printStackTrace()
                    return@exceptionally false
                }.get()
        if (!canDelete) return CompletableFuture.completedFuture(false)
        return db.deleteFrom(CLOUD_SERVERS).where(CLOUD_SERVERS.UNIQUE_ID.eq(server.uniqueId)).executeAsync()
            .toCompletableFuture().thenApply {
                return@thenApply it > 0 && remove(server)
            }.exceptionally {
                it.printStackTrace()
                return@exceptionally false
            }
    }

    override fun save(element: Server) {
        try {
            val server = firstOrNull { it.uniqueId == element.uniqueId }
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
                CLOUD_SERVERS.GROUP_NAME,
                CLOUD_SERVERS.HOST_ID,
                CLOUD_SERVERS.NUMERICAL_ID,
                CLOUD_SERVERS.TEMPLATE_ID,
                CLOUD_SERVERS.IP,
                CLOUD_SERVERS.PORT,
                CLOUD_SERVERS.MINIMUM_MEMORY,
                CLOUD_SERVERS.MAXIMUM_MEMORY,
                CLOUD_SERVERS.PLAYER_COUNT,
                CLOUD_SERVERS.STATE,
            ).values(
                element.uniqueId,
                element.group,
                element.host,
                element.numericalId,
                element.templateId,
                element.ip,
                element.port.toInt(),
                element.minMemory.toInt(),
                element.maxMemory.toInt(),
                element.playerCount.toInt(),
                element.state.toString()
            ).onDuplicateKeyUpdate().set(CLOUD_SERVERS.UNIQUE_ID, element.uniqueId).executeAsync()
            element.properties.forEach {
                db.insertInto(
                    CLOUD_SERVER_PROPERTIES,

                    CLOUD_SERVER_PROPERTIES.SERVER_ID,
                    CLOUD_SERVER_PROPERTIES.KEY,
                    CLOUD_SERVER_PROPERTIES.VALUE
                ).values(
                    element.uniqueId,
                    it.key,
                    it.value
                ).onDuplicateKeyUpdate().set(CLOUD_SERVER_PROPERTIES.SERVER_ID, element.uniqueId).executeAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println(e.message)
        }
    }

}