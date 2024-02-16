package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.GroupDefinition

class GroupRepository : Repository<Group>() {

    fun findGroupByName(name: String): GroupDefinition? {
        return firstOrNull { it.name == name }?.toDefinition()
    }

    override fun load() {
        TODO("Not yet implemented")
    }

}