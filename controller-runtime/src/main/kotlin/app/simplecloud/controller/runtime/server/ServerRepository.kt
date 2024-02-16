package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.shared.proto.ServerDefinition
import app.simplecloud.controller.shared.server.Server

class ServerRepository: Repository<Server>() {

    fun findServerById(id: String): ServerDefinition? {
        return firstOrNull { it.id == id }?.toDefinition()
    }

    fun findServersByHostId(id: String): List<ServerDefinition> {
        return filter { it.host == id }.map { it.toDefinition() }
    }

    fun findServersByGroup(group: String): List<ServerDefinition> {
        return filter { server -> server.group == group }.map { server -> server.toDefinition() }
    }

    override fun load() {
        TODO("Not yet implemented")
    }

}