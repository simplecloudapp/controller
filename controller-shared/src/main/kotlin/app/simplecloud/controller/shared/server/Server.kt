package app.simplecloud.controller.shared.server

import build.buf.gen.simplecloud.controller.v1.ServerDefinition
import build.buf.gen.simplecloud.controller.v1.ServerState
import build.buf.gen.simplecloud.controller.v1.ServerType
import java.time.LocalDateTime

data class Server(
    val uniqueId: String,
    val type: ServerType,
    val group: String,
    val host: String?,
    val numericalId: Int,
    val ip: String,
    val port: Long,
    val minMemory: Long,
    val maxMemory: Long,
    val maxPlayers: Long,
    var playerCount: Long,
    val properties: MutableMap<String, String>,
    var state: ServerState,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    fun toDefinition(): ServerDefinition {
        return ServerDefinition.newBuilder()
            .setUniqueId(uniqueId)
            .setType(type)
            .setGroupName(group)
            .setHostId(host)
            .setIp(ip)
            .setPort(port)
            .setState(state)
            .setMinimumMemory(minMemory)
            .setMaximumMemory(maxMemory)
            .setPlayerCount(playerCount)
            .setMaxPlayers(maxPlayers)
            .putAllProperties(properties)
            .setNumericalId(numericalId)
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()
    }

    fun toEnv(): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        map["SIMPLECLOUD_GROUP"] = this.group
        map["SIMPLECLOUD_HOST"] = this.host ?: "unknown"
        map["SIMPLECLOUD_IP"] = this.ip
        map["SIMPLECLOUD_PORT"] = this.port.toString()
        map["SIMPLECLOUD_UNIQUE_ID"] = this.uniqueId
        map["SIMPLECLOUD_CREATED_AT"] = this.createdAt.toString()
        map["SIMPLECLOUD_MAX_PLAYERS"] = this.maxPlayers.toString()
        map["SIMPLECLOUD_NUMERICAL_ID"] = this.numericalId.toString()
        map["SIMPLECLOUD_TYPE"] = this.type.toString()
        map["SIMPLECLOUD_MAX_MEMORY"] = this.maxMemory.toString()
        map["SIMPLECLOUD_MIN_MEMORY"] = this.minMemory.toString()
        map.putAll(this.properties.map {
            "SIMPLECLOUD_${
                it.key.uppercase().replace(" ", "_").replace("-", "_")
            }" to it.value
        })
        return map
    }

    companion object {
        @JvmStatic
        fun fromDefinition(serverDefinition: ServerDefinition): Server {
            return Server(
                serverDefinition.uniqueId,
                serverDefinition.type,
                serverDefinition.groupName,
                serverDefinition.hostId,
                serverDefinition.numericalId,
                serverDefinition.ip,
                serverDefinition.port,
                serverDefinition.minimumMemory,
                serverDefinition.maximumMemory,
                serverDefinition.maxPlayers,
                serverDefinition.playerCount,
                serverDefinition.propertiesMap,
                serverDefinition.state,
                LocalDateTime.parse(serverDefinition.createdAt),
                LocalDateTime.parse(serverDefinition.updatedAt)
            )
        }

        @JvmStatic
        fun fromDefinition(definitions: List<ServerDefinition>): List<Server> {
            return definitions.map { definition -> fromDefinition(definition) }
        }
    }
}