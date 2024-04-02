package app.simplecloud.controller.runtime.group

import app.simplecloud.controller.runtime.YamlDirectoryRepository
import app.simplecloud.controller.shared.group.Group
import java.nio.file.Path

class GroupRepository(
    path: Path
) : YamlDirectoryRepository<String, Group>(path, Group::class.java) {
    override fun getFileName(identifier: String): String {
        return "$identifier.yml"
    }

    override fun find(identifier: String): Group? {
        return entities.values.find { it.name == identifier }
    }

    override fun save(entity: Group) {
        save(getFileName(entity.name), entity)
    }
}