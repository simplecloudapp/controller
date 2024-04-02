package app.simplecloud.controller.shared.group

import app.simplecloud.controller.shared.proto.GroupDefinition
import app.simplecloud.controller.shared.proto.ServerType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class Group(
    val name: String = "",
    val type: ServerType = ServerType.OTHER,
    val serverUrl: String = "",
    val minMemory: Long = 0,
    val maxMemory: Long = 0,
    val startPort: Long = 0,
    @Transient val onlineServers: Long? = 0,
    val minOnlineCount: Long = 0,
    val maxOnlineCount: Long = 0,
    val properties: Map<String, String> = mapOf()
) {

    fun toDefinition(): GroupDefinition {
        return GroupDefinition.newBuilder()
            .setName(name)
            .setType(type)
            .setServerUrl(serverUrl)
            .setMinimumMemory(minMemory)
            .setMaximumMemory(maxMemory)
            .setStartPort(startPort)
            .setOnlineServers(onlineServers ?: 0)
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
