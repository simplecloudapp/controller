package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.GroupDefinition

class GroupRepository {

    // TODO: Load groups from file
    private val groups = listOf<Group>()

    fun findGroupByName(name: String): GroupDefinition? {
        return groups.firstOrNull { it.name == name }?.toDefinition()
    }

}