package app.simplecloud.controller.runtime.host

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.host.ServerHost
import io.grpc.ConnectivityState
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ServerHostRepository : Repository<ServerHost>() {

    private val hosts: ConcurrentHashMap<String, ServerHost> = ConcurrentHashMap()

    fun findServerHostById(id: String): ServerHost? {
        return hosts.getOrDefault(hosts.keys.firstOrNull { it == id }, null)
    }

    fun add(serverHost: ServerHost) {
        hosts[serverHost.id] = serverHost
    }

    fun remove(serverHost: ServerHost) {
        hosts.remove(serverHost.id, serverHost)
    }

    fun findLaziestServerHost(serverRepository: ServerRepository): ServerHost? {
        var lastAmount = Int.MAX_VALUE
        var lastHost: ServerHost? = null
        for (host: ServerHost in hosts.values) {
            val amount = serverRepository.findServersByHostId(host.id).size
            if (amount < lastAmount) {
                lastAmount = amount
                lastHost = host
            }
        }
        return lastHost
    }

    fun areServerHostsAvailable(): Boolean {
        return hosts.any {
            val channel = it.value.createChannel()
            val state = channel.getState(true)
            channel.shutdown()
            state == ConnectivityState.IDLE || state == ConnectivityState.READY
        }
    }

    override fun load() {
        throw UnsupportedOperationException("This method is not implemented.")
    }

    override fun delete(element: ServerHost): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(hosts.remove(element.id, element))
    }

    override fun save(element: ServerHost) {
        throw UnsupportedOperationException("This method is not implemented.")
    }

}

