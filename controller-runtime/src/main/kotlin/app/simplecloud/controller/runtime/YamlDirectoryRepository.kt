package app.simplecloud.controller.runtime

import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.loader.ParsingException
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.*


abstract class YamlDirectoryRepository<E, I>(
    private val directory: Path,
    private val clazz: Class<E>,
) : LoadableRepository<E, I> {

    private val logger = LogManager.getLogger(this::class.java)

    private val watchService = FileSystems.getDefault().newWatchService()
    private val loaders = mutableMapOf<File, YamlConfigurationLoader>()
    protected val entities = mutableMapOf<File, E>()

    abstract fun getFileName(identifier: I): String

    override suspend fun delete(element: E): Boolean {
        val file = entities.keys.find { entities[it] == element } ?: return false
        return deleteFile(file)
    }

    override suspend fun getAll(): List<E> {
        return entities.values.toList()
    }

    override fun load(): List<E> {
        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
        }

        registerWatcher()

        return Files.list(directory)
            .toList()
            .filter { !it.toFile().isDirectory && it.toString().endsWith(".yml") }
            .mapNotNull { load(it.toFile()) }
    }

    private fun load(file: File): E? {
        try {
            val loader = getOrCreateLoader(file)
            val node = loader.load(ConfigurationOptions.defaults())
            val entity = node.get(clazz) ?: return null
            entities[file] = entity
            return entity
        } catch (ex: ParsingException) {
            val existedBefore = entities.containsKey(file)
            if (existedBefore) {
                logger.error("Could not load file ${file.name}. Switching back to an older version.")
                return null
            }

            logger.error("Could not load file ${file.name}. Make sure it's correctly formatted!")
            return null
        }
    }

    private fun deleteFile(file: File): Boolean {
        val deletedSuccessfully = file.delete()
        val removedSuccessfully = entities.remove(file) != null
        return deletedSuccessfully && removedSuccessfully
    }

    protected fun save(fileName: String, entity: E) {
        val file = directory.resolve(fileName).toFile()
        val loader = getOrCreateLoader(file)
        val node = loader.createNode(ConfigurationOptions.defaults())
        node.set(clazz, entity)
        loader.save(node)
        entities[file] = entity
    }

    private fun getOrCreateLoader(file: File): YamlConfigurationLoader {
        return loaders.getOrPut(file) {
            YamlConfigurationLoader.builder()
                .path(file.toPath())
                .nodeStyle(NodeStyle.BLOCK)
                .defaultOptions { options ->
                    options.serializers { builder ->
                        builder.registerAnnotatedObjects(objectMapperFactory())
                    }
                }.build()
        }
    }

    private fun registerWatcher(): Job {
        directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        return CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val key = watchService.take()
                for (event in key.pollEvents()) {
                    val path = event.context() as? Path ?: continue
                    val resolvedPath = directory.resolve(path)
                    if (Files.isDirectory(resolvedPath) || !resolvedPath.toString().endsWith(".yml")) {
                        continue
                    }
                    val kind = event.kind()
                    logger.info("Detected change in $resolvedPath (${getChangeStatus(kind)})")
                    when (kind) {
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY
                            -> {
                            load(resolvedPath.toFile())
                        }

                        StandardWatchEventKinds.ENTRY_DELETE -> {
                            deleteFile(resolvedPath.toFile())
                        }
                    }
                }
                key.reset()
            }
        }
    }

    private fun getChangeStatus(kind: WatchEvent.Kind<*>): String {
        return when (kind) {
            StandardWatchEventKinds.ENTRY_CREATE -> "Created"
            StandardWatchEventKinds.ENTRY_DELETE -> "Deleted"
            StandardWatchEventKinds.ENTRY_MODIFY -> "Modified"
            else -> "Unknown"
        }
    }

}