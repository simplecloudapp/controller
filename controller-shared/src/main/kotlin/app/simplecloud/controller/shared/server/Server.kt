package app.simplecloud.controller.shared.server

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.controller.shared.proto.ServerDefinition
import java.util.*

data class Server(
        val id: String,
        val host: String?,
        val group: String,
) {
    fun toDefinition(): ServerDefinition {
        return ServerDefinition.newBuilder()
                .setUniqueId(id)
                .setGroupName(group)
                .build()
    }

    companion object {
        @JvmStatic
        fun fromDefinition(serverDefinition: ServerDefinition): Server {
            return Server(
                    serverDefinition.uniqueId,
                    serverDefinition.hostId,
                    serverDefinition.groupName
            )
        }


        @JvmStatic
        fun create(group: Group, serverHost: ServerHost): Server {
            return Server(
                    UUID.randomUUID().toString(),
                    serverHost.getId(),
                    group.name,
            )
        }

        @JvmStatic
        fun fromDefinition(definitions: List<ServerDefinition>): List<Server> {
            return definitions.map { definition -> fromDefinition(definition) }
        }
    }
}