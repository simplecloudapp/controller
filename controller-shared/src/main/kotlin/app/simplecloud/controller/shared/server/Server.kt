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