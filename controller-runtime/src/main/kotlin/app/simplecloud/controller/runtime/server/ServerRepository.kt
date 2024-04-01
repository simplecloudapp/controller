package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.database.Database
import app.simplecloud.controller.shared.db.Tables.CLOUD_SERVERS
import app.simplecloud.controller.shared.db.Tables.CLOUD_SERVER_PROPERTIES
import app.simplecloud.controller.shared.proto.ServerState
import app.simplecloud.controller.shared.proto.ServerType
import app.simplecloud.controller.shared.server.Server
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class ServerRepository(
  private val database: Database,
  private val numericalIdRepository: ServerNumericalIdRepository
) : Repository<Server>() {

  fun findServerById(id: String): Server? {
    return firstOrNull { it.uniqueId == id }
  }

  fun findServersByHostId(id: String): List<Server> {
    return filter { it.host == id }
  }

  fun findServersByGroup(group: String): List<Server> {
    return filter { server -> server.group == group }
  }

  fun findServersByType(type: ServerType): List<Server> {
    return filter { server -> server.type == type }
  }

  override fun load() {
    clear()
    val query = database.context.select().from(CLOUD_SERVERS).fetchInto(CLOUD_SERVERS)
    query.map {
      val propertiesQuery = database.context.select().from(CLOUD_SERVER_PROPERTIES).fetchInto(CLOUD_SERVER_PROPERTIES)
      numericalIdRepository.saveNumericalId(it.groupName, it.numericalId)
      add(
        Server(
          it.uniqueId,
          ServerType.valueOf(it.type),
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
          ServerState.valueOf(it.state),
          it.createdAt,
          it.updatedAt
        )
      )
    }
  }

  override fun delete(element: Server): CompletableFuture<Boolean> {
    val server = firstOrNull { it.uniqueId == element.uniqueId } ?: return CompletableFuture.completedFuture(false)
    val canDelete =
      database.context.deleteFrom(CLOUD_SERVER_PROPERTIES).where(CLOUD_SERVER_PROPERTIES.SERVER_ID.eq(server.uniqueId))
        .executeAsync().toCompletableFuture().thenApply {
          return@thenApply true
        }.exceptionally {
          it.printStackTrace()
          return@exceptionally false
        }.get()
    if (!canDelete) return CompletableFuture.completedFuture(false)
    numericalIdRepository.removeNumericalId(server.group, server.numericalId)
    return database.context.deleteFrom(CLOUD_SERVERS).where(CLOUD_SERVERS.UNIQUE_ID.eq(server.uniqueId)).executeAsync()
      .toCompletableFuture().thenApply {
        return@thenApply it > 0 && remove(server)
      }.exceptionally {
        it.printStackTrace()
        return@exceptionally false
      }
  }

  override fun save(element: Server) {
    val server = firstOrNull { it.uniqueId == element.uniqueId }
    if (server != null) {
      val index = indexOf(server)
      removeAt(index)
      add(index, element)
    } else {
      add(element)
    }

    numericalIdRepository.saveNumericalId(element.group, element.numericalId)

    val currentTimestamp = LocalDateTime.now()
    database.context.insertInto(
      CLOUD_SERVERS,

      CLOUD_SERVERS.UNIQUE_ID,
      CLOUD_SERVERS.TYPE,
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
      CLOUD_SERVERS.CREATED_AT,
      CLOUD_SERVERS.UPDATED_AT
    )
      .values(
        element.uniqueId,
        element.type.toString(),
        element.group,
        element.host,
        element.numericalId,
        element.templateId,
        element.ip,
        element.port.toInt(),
        element.minMemory.toInt(),
        element.maxMemory.toInt(),
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
      .set(CLOUD_SERVERS.TEMPLATE_ID, element.templateId)
      .set(CLOUD_SERVERS.IP, element.ip)
      .set(CLOUD_SERVERS.PORT, element.port.toInt())
      .set(CLOUD_SERVERS.MINIMUM_MEMORY, element.minMemory.toInt())
      .set(CLOUD_SERVERS.MAXIMUM_MEMORY, element.maxMemory.toInt())
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

}