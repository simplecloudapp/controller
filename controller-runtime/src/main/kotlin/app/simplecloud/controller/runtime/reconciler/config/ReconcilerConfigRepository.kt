package app.simplecloud.controller.runtime.reconciler.config

import app.simplecloud.controller.runtime.YamlDirectoryRepository
import kotlin.io.path.Path

/**
 * @author Niklas Nieberler
 */

class ReconcilerConfigRepository : YamlDirectoryRepository<ReconcilerConfig, String>(
    Path("reconcilers"),
    ReconcilerConfig::class.java
) {
    override fun getFileName(identifier: String): String {
        return "$identifier.yml"
    }

    override fun save(element: ReconcilerConfig) {
        save(getFileName(element.name), element)
    }

    override suspend fun find(identifier: String): ReconcilerConfig? {
        return entities.values.find { it.name == identifier }
    }
}