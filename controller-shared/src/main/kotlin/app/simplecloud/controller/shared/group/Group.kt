package app.simplecloud.controller.shared.group

import app.simplecloud.controller.shared.proto.GroupDefinition
import app.simplecloud.controller.shared.proto.ServerType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class Group(
        val name: String,
        val type: ServerType,
        val serverUrl: String,
        val minMemory: Long,
        val maxMemory: Long,
        val startPort: Long,
        @Transient val onlineServers: Long?,
        val minOnlineCount: Long,
        val maxOnlineCount: Long,
        val properties: Map<String, String>,
) {

    fun toDefinition(): GroupDefinition {
        return GroupDefinition.newBuilder()
            .setName(name)
            .setType(type)
            .setServerUrl(serverUrl)
            .setMinimumMemory(minMemory)
            .setMaximumMemory(maxMemory)
            .setStartPort(startPort)
            .setOnlineServers(onlineServers?: 0)
            .setMinimumOnlineCount(minOnlineCount)
            .setMaximumOnlineCount(maxOnlineCount)
            .putAllProperties(properties)
            .build()
    }

    companion object {
        @JvmStatic
        fun fromDefinition(groupDefinition: GroupDefinition): Group {
            return Group(
                groupDefinition.name,
                groupDefinition.type,
                groupDefinition.serverUrl,
                groupDefinition.minimumMemory,
                groupDefinition.maximumMemory,
                groupDefinition.startPort,
                groupDefinition.onlineServers,
                groupDefinition.minimumOnlineCount,
                groupDefinition.maximumOnlineCount,
                groupDefinition.propertiesMap
            )
        }
    }

}
