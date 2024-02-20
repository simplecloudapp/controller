package app.simplecloud.controller.runtime.host

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.host.ServerHost
import java.util.concurrent.CompletableFuture

class ServerHostRepository : Repository<ServerHost>() {

    fun findServerHostById(id: String): ServerHost? {
        return firstOrNull { it.getId() == id }
    }

    fun findLaziestServerHost(serverRepository: ServerRepository): ServerHost? {
        var lastAmount = Int.MAX_VALUE
        var lastHost: ServerHost? = null
        for (host: ServerHost in this) {
            val amount = serverRepository.findServersByHostId(host.getId()).size
            if (amount < lastAmount) {
                lastAmount = amount
                lastHost = host
            }
        }
        return lastHost
    }

    override fun load() {
        TODO("Not yet implemented")
    }

    override fun delete(element: ServerHost): CompletableFuture<Boolean> {
        throw UnsupportedOperationException("delete is not available on this repository")
    }

    override fun save(element: ServerHost) {
        throw UnsupportedOperationException("delete is not available on this repository")
    }
}

