package app.simplecloud.controller.runtime.droplet

import app.simplecloud.controller.runtime.Repository
import app.simplecloud.droplet.api.droplet.Droplet

class DropletRepository : Repository<Droplet, String> {

    private val currentDroplets = mutableListOf<Droplet>()

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
        val droplet = find(element.type, element.id)
        if (droplet != null) {
            currentDroplets[currentDroplets.indexOf(droplet)] = element
            return
        }
        currentDroplets.add(element)
    }

    override suspend fun delete(element: Droplet): Boolean {
        return currentDroplets.remove(element)
    }
}