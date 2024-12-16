package app.simplecloud.controller.runtime.droplet

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.envoy.DropletCache
import app.simplecloud.controller.runtime.host.ServerHostRepository
import app.simplecloud.controller.runtime.server.ServerHostAttacher
import app.simplecloud.controller.shared.host.ServerHost
import app.simplecloud.droplet.api.auth.AuthCallCredentials
import app.simplecloud.droplet.api.droplet.Droplet
import build.buf.gen.simplecloud.controller.v1.ServerHostServiceGrpcKt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DropletRepository(
    private val authCallCredentials: AuthCallCredentials,
    private val serverHostAttacher: ServerHostAttacher,
    private val serverHostRepository: ServerHostRepository,
) : Repository<Droplet, String> {

    private val currentDroplets = mutableListOf<Droplet>()
    private val dropletCache = DropletCache(this)

    override suspend fun getAll(): List<Droplet> {
        return currentDroplets
    }

    override suspend fun find(identifier: String): Droplet? {
        return currentDroplets.firstOrNull { it.id == identifier }
    }

    fun find(type: String, identifier: String): Droplet? {
        return currentDroplets.firstOrNull { it.type == type && it.id == identifier }
    }

    override fun save(element: Droplet) {
        val updated = managePortRange(element)
        val droplet = find(element.type, element.id)
        if (droplet != null) {
            currentDroplets[currentDroplets.indexOf(droplet)] = updated
            postUpdate(updated)
            return
        }
        currentDroplets.add(updated)
        postUpdate(updated)
    }

    private fun postUpdate(droplet: Droplet) {
        CoroutineScope(Dispatchers.IO).launch {
            dropletCache.update()
            if (droplet.type != "serverhost") return@launch
            serverHostAttacher.attach(
                ServerHost(
                    droplet.id, droplet.host, droplet.port, ServerHostServiceGrpcKt.ServerHostServiceCoroutineStub(
                        ServerHost.createChannel(
                            droplet.host,
                            droplet.port
                        )
                    ).withCallCredentials(authCallCredentials)
                )
            )
        }
    }

    private fun managePortRange(element: Droplet): Droplet {
        if (!currentDroplets.any { it.envoyPort == element.envoyPort }) return element
        return managePortRange(element.copy(envoyPort = element.envoyPort + 1))
    }

    override suspend fun delete(element: Droplet): Boolean {
        val found = find(element.type, element.id) ?: return false
        if (!currentDroplets.remove(found)) return false
        dropletCache.update()
        if (element.type == "serverhost") {
            val host = serverHostRepository.findServerHostById(element.id) ?: return true
            serverHostRepository.delete(host)
        }
        return true
    }

    fun getAsDropletCache(): DropletCache {
        return dropletCache
    }
}