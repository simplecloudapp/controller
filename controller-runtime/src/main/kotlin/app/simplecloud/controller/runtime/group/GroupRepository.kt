package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.proto.GroupDefinition
import java.util.concurrent.CompletableFuture

class GroupRepository : Repository<Group>() {

    fun findGroupByName(name: String): GroupDefinition? {
        return firstOrNull { it.name == name }?.toDefinition()
    }

    override fun load() {
        TODO("Not yet implemented")
    }

    override fun delete(element: Group): CompletableFuture<Boolean> {
        throw UnsupportedOperationException("delete is not available on this repository")
    }


    override fun save(element: Group) {
        throw UnsupportedOperationException("delete is not available on this repository")
    }

}