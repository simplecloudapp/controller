package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.runtime.YamlDirectoryRepository
import app.simplecloud.controller.shared.group.Group
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class GroupRepository(
    path: Path
) : YamlDirectoryRepository<Group, String>(path, Group::class.java) {
    override fun getFileName(identifier: String): String {
        return "$identifier.yml"
    }

    override fun find(identifier: String): CompletableFuture<Group?> {
        return CompletableFuture.completedFuture(entities.values.find { it.name == identifier })
    }

    override fun save(element: Group) {
        save(getFileName(element.name), element)
    }

    override fun getAll(): CompletableFuture<List<Group>> {
        return CompletableFuture.completedFuture(entities.values.toList())
    }
}