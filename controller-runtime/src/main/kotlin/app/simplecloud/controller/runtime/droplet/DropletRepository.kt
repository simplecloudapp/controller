package app.simplecloud.controller.runtime.droplet

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.controller.runtime.envoy.DropletCache
import app.simplecloud.droplet.api.droplet.Droplet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DropletRepository : Repository<Droplet, String> {

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
            return
        }
        currentDroplets.add(updated)
        CoroutineScope(Dispatchers.IO).launch {
            dropletCache.update()
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
        return true
    }

    fun getAsDropletCache(): DropletCache {
        return dropletCache
    }
}