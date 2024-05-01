package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.LoadableRepository
import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.shared.db.Tables.CLOUD_SERVERS
import app.simplecloud.controller.shared.db.Tables.CLOUD_SERVER_PROPERTIES
import app.simplecloud.controller.shared.db.tables.records.CloudServersRecord
import app.simplecloud.controller.shared.server.Server
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerType
import org.jooq.Result
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class ServerRepository(
    private val database: Database,
    private val numericalIdRepository: ServerNumericalIdRepository
) : LoadableRepository<Server, String, Unit> {


    override fun find(identifier: String): CompletableFuture<Server?> {
        return CompletableFuture.supplyAsync {
            val query = database.context.select()
                .from(CLOUD_SERVERS)
                .where(CLOUD_SERVERS.UNIQUE_ID.eq(identifier))
                .fetchInto(
                    CLOUD_SERVERS
                )
            return@supplyAsync toList(query).firstOrNull()
        }
    }


    fun findServerByNumerical(group: String, id: Int): CompletableFuture<Server?> {
        return CompletableFuture.supplyAsync {
            val query = database.context.select().from(CLOUD_SERVERS)
                .where(
                    CLOUD_SERVERS.GROUP_NAME.eq(group)
                        .and(CLOUD_SERVERS.NUMERICAL_ID.eq(id))
                )
                .fetchInto(CLOUD_SERVERS)
            return@supplyAsync toList(query).firstOrNull()
        }
    }

    override fun getAll(): CompletableFuture<List<Server>> {
        return CompletableFuture.supplyAsync {
            val query = database.context.select()
                .from(CLOUD_SERVERS)
                .fetchInto(CLOUD_SERVERS)
            return@supplyAsync toList(query)
        }
    }

    fun findServersByHostId(id: String): CompletableFuture<List<Server>> {
        return CompletableFuture.supplyAsync {
            val query = database.context.select()
                .from(CLOUD_SERVERS)
                .where(CLOUD_SERVERS.HOST_ID.eq(id))
                .fetchInto(
                    CLOUD_SERVERS
                )
            return@supplyAsync toList(query)
        }
    }

    fun findServersByGroup(group: String): CompletableFuture<List<Server>> {
        return CompletableFuture.supplyAsync {
            val query = database.context.select()
                .from(CLOUD_SERVERS)
                .where(CLOUD_SERVERS.GROUP_NAME.eq(group))
                .fetchInto(
                    CLOUD_SERVERS
                )
            return@supplyAsync toList(query)
        }

    }

    fun findServersByType(type: ServerType): CompletableFuture<List<Server>> {
        return CompletableFuture.supplyAsync {
            val query = database.context.select()
                .from(CLOUD_SERVERS)
                .where(CLOUD_SERVERS.TYPE.eq(type.toString()))
                .fetchInto(CLOUD_SERVERS)
            return@supplyAsync toList(query)
        }
    }

    private fun toList(query: Result<CloudServersRecord>): List<Server> {
        val result = mutableListOf<Server>()
        query.map {
            val propertiesQuery =
                database.context.select()
                    .from(CLOUD_SERVER_PROPERTIES)
                    .where(CLOUD_SERVER_PROPERTIES.SERVER_ID.eq(it.uniqueId))
                    .fetchInto(CLOUD_SERVER_PROPERTIES)
            result.add(
                Server(
                    it.uniqueId,
                    ServerType.valueOf(it.type),
                    it.groupName,
                    it.hostId,
                    it.numericalId,
                    it.ip,
                    it.port.toLong(),
                    it.minimumMemory.toLong(),
                    it.maximumMemory.toLong(),
                    it.maxPlayers.toLong(),
                    it.playerCount.toLong(),
                    propertiesQuery.map { item ->
                        item.key to item.value
                    }.toMap().toMutableMap(),
                    ServerState.valueOf(it.state),
                    it.createdAt,
                    it.updatedAt
                )
            )
        }
        return result
    }

    override fun delete(element: Server): CompletableFuture<Boolean> {
        val canDelete =
            database.context.deleteFrom(CLOUD_SERVER_PROPERTIES)
                .where(CLOUD_SERVER_PROPERTIES.SERVER_ID.eq(element.uniqueId))
                .executeAsync().toCompletableFuture().thenApply {
                    return@thenApply true
                }.exceptionally {
                    it.printStackTrace()
                    return@exceptionally false
                }.get()
        if (!canDelete) return CompletableFuture.completedFuture(false)
        numericalIdRepository.removeNumericalId(element.group, element.numericalId)
        return database.context.deleteFrom(CLOUD_SERVERS)
            .where(CLOUD_SERVERS.UNIQUE_ID.eq(element.uniqueId))
            .executeAsync()
            .toCompletableFuture().thenApply {
                return@thenApply it > 0
            }.exceptionally {
                it.printStackTrace()
                return@exceptionally false
            }
    }

    @Synchronized
    override fun save(element: Server) {
        numericalIdRepository.saveNumericalId(element.group, element.numericalId)

        val currentTimestamp = LocalDateTime.now()
        database.context.insertInto(
            CLOUD_SERVERS,

            CLOUD_SERVERS.UNIQUE_ID,
            CLOUD_SERVERS.TYPE,
            CLOUD_SERVERS.GROUP_NAME,
            CLOUD_SERVERS.HOST_ID,
            CLOUD_SERVERS.NUMERICAL_ID,
            CLOUD_SERVERS.IP,
            CLOUD_SERVERS.PORT,
            CLOUD_SERVERS.MINIMUM_MEMORY,
            CLOUD_SERVERS.MAXIMUM_MEMORY,
            CLOUD_SERVERS.MAX_PLAYERS,
            CLOUD_SERVERS.PLAYER_COUNT,
            CLOUD_SERVERS.STATE,
            CLOUD_SERVERS.CREATED_AT,
            CLOUD_SERVERS.UPDATED_AT
        )
            .values(
                element.uniqueId,
                element.type.toString(),
                element.group,
                element.host,
                element.numericalId,
                element.ip,
                element.port.toInt(),
                element.minMemory.toInt(),
                element.maxMemory.toInt(),
                element.maxPlayers.toInt(),
                element.playerCount.toInt(),
                element.state.toString(),
                currentTimestamp,
                currentTimestamp
            )
            .onDuplicateKeyUpdate()
            .set(CLOUD_SERVERS.UNIQUE_ID, element.uniqueId)
            .set(CLOUD_SERVERS.TYPE, element.type.toString())
            .set(CLOUD_SERVERS.GROUP_NAME, element.group)
            .set(CLOUD_SERVERS.HOST_ID, element.host)
            .set(CLOUD_SERVERS.NUMERICAL_ID, element.numericalId)
            .set(CLOUD_SERVERS.IP, element.ip)
            .set(CLOUD_SERVERS.PORT, element.port.toInt())
            .set(CLOUD_SERVERS.MINIMUM_MEMORY, element.minMemory.toInt())
            .set(CLOUD_SERVERS.MAXIMUM_MEMORY, element.maxMemory.toInt())
            .set(CLOUD_SERVERS.MAX_PLAYERS, element.maxPlayers.toInt())
            .set(CLOUD_SERVERS.PLAYER_COUNT, element.playerCount.toInt())
            .set(CLOUD_SERVERS.STATE, element.state.toString())
            .set(CLOUD_SERVERS.UPDATED_AT, currentTimestamp)
            .executeAsync()
        element.properties.forEach {
            database.context.insertInto(
                CLOUD_SERVER_PROPERTIES,

                CLOUD_SERVER_PROPERTIES.SERVER_ID,
                CLOUD_SERVER_PROPERTIES.KEY,
                CLOUD_SERVER_PROPERTIES.VALUE
            )
                .values(
                    element.uniqueId,
                    it.key,
                    it.value
                )
                .onDuplicateKeyUpdate()
                .set(CLOUD_SERVER_PROPERTIES.SERVER_ID, element.uniqueId)
                .set(CLOUD_SERVER_PROPERTIES.KEY, it.key)
                .set(CLOUD_SERVER_PROPERTIES.VALUE, it.value)
                .executeAsync()
        }
    }

    override fun load() {
        val query = database.context.select()
            .from(CLOUD_SERVERS)
            .fetchInto(CLOUD_SERVERS)
        val list = toList(query)
        list.forEach {
            numericalIdRepository.saveNumericalId(it.group, it.numericalId)
        }
    }

}