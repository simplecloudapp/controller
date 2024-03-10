package app.simplecloud.controller.runtime.host

import app.simplecloud.controller.runtime.YamlRepository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.host.ServerHost
import java.io.File

class ServerHostRepository(path: String) : YamlRepository<ServerHost>(path, ServerHost::class.java) {
    fun findServerHostById(id: String): ServerHost? {
        return firstOrNull { it.id == id }
    }

    fun findLaziestServerHost(serverRepository: ServerRepository): ServerHost? {
        var lastAmount = Int.MAX_VALUE
        var lastHost: ServerHost? = null
        for (host: ServerHost in this) {
            val amount = serverRepository.findServersByHostId(host.id).size
            if (amount < lastAmount) {
                lastAmount = amount
                lastHost = host
            }
        }
        return lastHost
    }

    override fun findIndex(element: ServerHost): Int {
        return indexOf(firstOrNull { it.id == element.id })
    }

}

