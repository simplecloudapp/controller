package app.simplecloud.controller.shared.group

import app.simplecloud.controller.shared.proto.GroupDefinition

data class Group(
    val name: String,
) {

    fun toDefinition(): GroupDefinition {
        return GroupDefinition.newBuilder()
            .setName(name)
            .build()
    }

    companion object {
        @JvmStatic
        fun fromDefinition(groupDefinition: GroupDefinition): Group {
            return Group(
                groupDefinition.name
            )
        }
    }

}
