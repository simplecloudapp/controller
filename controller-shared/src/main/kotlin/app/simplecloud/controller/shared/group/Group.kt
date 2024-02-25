package app.simplecloud.controller.shared.group

import app.simplecloud.controller.shared.proto.GroupDefinition

data class Group(
        val name: String,
        val templateName: String,
        val minMemory: Long,
        val maxMemory: Long,
        val startPort: Long,
        val onlineServers: Long,
        val minOnlineCount: Long,
        val maxOnlineCount: Long,
        val properties: Map<String, String>,
) {

    fun toDefinition(): GroupDefinition {
        return GroupDefinition.newBuilder()
                .setName(name)
                .setTemplateName(templateName)
                .setMinimumMemory(minMemory)
                .setMaximumMemory(maxMemory)
                .setStartPort(startPort)
                .setOnlineServers(onlineServers)
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
                    groupDefinition.templateName,
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
