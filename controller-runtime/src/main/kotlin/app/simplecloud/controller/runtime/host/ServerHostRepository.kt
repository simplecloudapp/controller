package app.simplecloud.controller.runtime.host

import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.host.ServerHost

class ServerHostRepository {

    // TODO: Load hosts from file
    private val hosts = listOf<ServerHost>()
    fun findServerHostById(id: String): ServerHost? {
        return hosts.firstOrNull { it.getId() == id }
    }

    fun findLaziestServerHost(serverRepository: ServerRepository): ServerHost? {
        var lastAmount = Int.MAX_VALUE
        var lastHost: ServerHost? = null
        for (host: ServerHost in hosts) {
            val amount = serverRepository.findServersByHostId(host.getId()).size
            if (amount < lastAmount) {
                lastAmount = amount
                lastHost = host
            }
        }
        return lastHost
    }
}

