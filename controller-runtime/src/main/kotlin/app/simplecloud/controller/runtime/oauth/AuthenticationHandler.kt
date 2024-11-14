package app.simplecloud.controller.runtime.oauth

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class AuthenticationHandler(
    private val groupRepository: AuthGroupRepository
) {
    suspend fun saveGroup(call: RoutingCall) {
        //TODO: permission check
        val params = call.receiveParameters()
        val groupName = params["group_name"]
        if (groupName == null) {
            call.respond(HttpStatusCode.BadRequest, "You must specify a group name")
            return
        }
        val scopes = params["scopes"]?.split(";") ?: emptyList()
        groupRepository.save(OAuthGroup(scopes, groupName))
        call.respond("Group successfully updated")
    }

    suspend fun getGroup(call: RoutingCall) {
        //TODO: permission check
        val group = loadGroup(call) ?: return
        call.respond(mapOf("group_name" to group.name, "scope" to group.scopes.joinToString(" ")))
    }

    suspend fun getGroups(call: RoutingCall) {
        //TODO: permission check
        val groups = groupRepository.getAll()
        call.respond(listOf(groups.map {
            mapOf(
                "group_name" to it.name,
                "scope" to it.scopes.joinToString(" ")
            )
        }).flatten())
    }

    suspend fun deleteGroup(call: RoutingCall) {
        //TODO: permission check
        val group = loadGroup(call) ?: return
        groupRepository.delete(group)
        call.respond("Group successfully deleted")
    }

    private suspend fun loadGroup(call: RoutingCall): OAuthGroup? {
        val params = call.receiveParameters()
        val groupName = params["group_name"]
        if (groupName == null) {
            call.respond(HttpStatusCode.BadRequest, "You must specify a group name")
            return null
        }
        val group = groupRepository.find(groupName)
        if (group == null) {
            call.respond(HttpStatusCode.NotFound, "Group not found")
            return null
        }
        return group
    }


    suspend fun createUser() {
        //TODO: permission check
    }

    suspend fun updateUser() {
        //TODO: permission check

    }

    suspend fun deleteUser() {
        //TODO: permission check

    }

    suspend fun login() {

    }
}