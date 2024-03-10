package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.runtime.YamlRepository
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.GroupDefinition

class GroupRepository(path: String) : YamlRepository<Group>(path, Group::class.java) {

    fun findGroupByName(name: String): GroupDefinition? {
        return firstOrNull { it.name == name }?.toDefinition()
    }

    override fun findIndex(element: Group): Int {
        return indexOf(firstOrNull { it.name == element.name })
    }


}