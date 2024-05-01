package app.simplecloud.controller.runtime.host

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.server.ServerRepository
import app.simplecloud.controller.shared.host.ServerHost
import io.grpc.ConnectivityState
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ServerHostRepository : Repository<ServerHost, ServerRepository> {

    private val hosts: ConcurrentHashMap<String, ServerHost> = ConcurrentHashMap()

    fun findServerHostById(id: String): ServerHost? {
        return hosts.getOrDefault(hosts.keys.firstOrNull { it == id }, null)
    }

    override fun save(element: ServerHost) {
        hosts[element.id] = element
    }

    override fun find(identifier: ServerRepository): CompletableFuture<ServerHost?> {
        return CompletableFuture.supplyAsync {
            return@supplyAsync hosts.values.minByOrNull { host ->
                identifier.findServersByHostId(host.id).thenApply { return@thenApply it.size }.get()
            }
        }
    }

    fun areServerHostsAvailable(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            hosts.any {
                val channel = it.value.createChannel()
                val state = channel.getState(true)
                channel.shutdown()
                state == ConnectivityState.IDLE || state == ConnectivityState.READY
            }
        }
    }

    override fun delete(element: ServerHost): CompletableFuture<Boolean> {
        return CompletableFuture.completedFuture(hosts.remove(element.id, element))
    }

    override fun getAll(): CompletableFuture<List<ServerHost>> {
        return CompletableFuture.completedFuture(hosts.values.toList())
    }

}

