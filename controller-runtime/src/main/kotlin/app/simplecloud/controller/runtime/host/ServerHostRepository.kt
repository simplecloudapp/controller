package app.simplecloud.controller.runtime.host

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.host.ServerHost
import io.grpc.ConnectivityState
import java.util.concurrent.CompletableFuture

class ServerHostRepository : Repository<ServerHost>() {
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

    fun areServerHostsAvailable(): Boolean {
        return any {
            val channel = it.createChannel()
            val state = channel.getState(true)
            channel.shutdown()
            state == ConnectivityState.IDLE || state == ConnectivityState.READY
        }
    }

    override fun load() {
        throw UnsupportedOperationException("This method is not implemented.")
    }

    override fun delete(element: ServerHost): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(add(element))
    }

    override fun save(element: ServerHost) {
        throw UnsupportedOperationException("This method is not implemented.")
    }

}

