package app.simplecloud.controller.runtime.server

import app.simplecloud.controller.shared.proto.ServerDefinition
import app.simplecloud.controller.shared.server.Server

class ServerRepository {
    // TODO: Load servers from db
    private val servers = listOf<Server>()

    fun findServerById(id: String): ServerDefinition? {
        return servers.firstOrNull { it.id == id }?.toDefinition()
    }

    fun findServersByHostId(id: String): List<ServerDefinition> {
        return servers.filter { it.host == id }.map { it.toDefinition() }
    }

    fun findServersByGroup(group: String): List<ServerDefinition> {
        return servers.filter { server -> server.group == group }.map { server -> server.toDefinition() }
    }
}