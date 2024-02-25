package app.simplecloud.controller.shared.server

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.proto.ServerDefinition
import app.simplecloud.controller.shared.proto.ServerState
import java.util.*
import kotlin.math.min

data class Server(
        val uniqueId: String,
        val group: String,
        val host: String?,
        val numericalId: Int,
        val templateId: String,
        val ip: String,
        val port: Long,
        val minMemory: Long,
        val maxMemory: Long,
        val playerCount: Long,
        val properties: Map<String, String>,
        val state: ServerState,
) {
    fun toDefinition(): ServerDefinition {
        return ServerDefinition.newBuilder()
                .setUniqueId(uniqueId)
                .setGroupName(group)
                .setHostId(host)
                .setIp(ip)
                .setPort(port)
                .setState(state)
                .setMinimumMemory(minMemory)
                .setMaximumMemory(maxMemory)
                .setPlayerCount(playerCount)
                .putAllProperties(properties)
                .setTemplateId(templateId)
                .setNumericalId(numericalId)
                .build()
    }

    companion object {
        @JvmStatic
        fun fromDefinition(serverDefinition: ServerDefinition): Server {
            return Server(
                    serverDefinition.uniqueId,
                    serverDefinition.hostId,
                    serverDefinition.groupName,
                    serverDefinition.numericalId,
                    serverDefinition.templateId,
                    serverDefinition.ip,
                    serverDefinition.port,
                    serverDefinition.minimumMemory,
                    serverDefinition.maximumMemory,
                    serverDefinition.playerCount,
                    serverDefinition.propertiesMap,
                    serverDefinition.state
            )
        }

        @JvmStatic
        fun fromDefinition(definitions: List<ServerDefinition>): List<Server> {
            return definitions.map { definition -> fromDefinition(definition) }
        }
    }
}