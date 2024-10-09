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

    override suspend fun find(identifier: String): Group? {
        return entities.values.find { it.name == identifier }
    }

    override fun save(element: Group) {
        save(getFileName(element.name), element)
    }

    override suspend fun getAll(): List<Group> {
        return entities.values.toList()
    }
}