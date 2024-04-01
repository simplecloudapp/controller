package app.simplecloud.controller.runtime

import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

abstract class YamlDirectoryRepository<I, T>(
  private val directory: Path,
  private val clazz: Class<T>,
) {

  private val logger = LogManager.getLogger(this::class.java)

  private val watchService = FileSystems.getDefault().newWatchService()
  private val loaders = mutableMapOf<File, YamlConfigurationLoader>()
  protected val entities = mutableMapOf<File, T>()

  abstract fun getFileName(identifier: I): String

  abstract fun find(identifier: I): T?

  abstract fun save(entity: T)

  fun delete(entity: T): Boolean {
    val file = entities.keys.find { entities[it] == entity } ?: return false
    return delete(file)
  }

  fun findAll(): List<T> {
    return entities.values.toList()
  }

  fun loadAll() {
    logger.info("Loading all entities from $directory")
    if (!directory.toFile().exists()) {
      directory.toFile().mkdir()
    }

    Files.list(directory)
      .filter { !it.toFile().isDirectory && it.toString().endsWith(".yml") }
      .forEach {
        load(it.toFile())
      }

    registerWatcher()
  }

  private fun load(file: File) {
    val loader = getOrCreateLoader(file)
    val node = loader.load(ConfigurationOptions.defaults())
    val entity = node.get(clazz) ?: return
    entities[file] = entity
    logger.info("Loaded $file")
  }

  private fun delete(file: File): Boolean {
    val deletedSuccessfully = file.delete()
    val removedSuccessfully = entities.remove(file) != null
    return deletedSuccessfully && removedSuccessfully
  }

  protected fun save(fileName: String, entity: T) {
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
        .defaultOptions { options ->
          options.serializers { builder ->
            builder.registerAnnotatedObjects(objectMapperFactory())
          }
        }.build()
    }
  }

  @OptIn(InternalCoroutinesApi::class)
  private fun registerWatcher(): Job {
    directory.register(
      watchService,
      StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_DELETE,
      StandardWatchEventKinds.ENTRY_MODIFY
    )

    return CoroutineScope(Dispatchers.Default).launch {
      while (NonCancellable.isActive) {
        val key = watchService.take()
        for (event in key.pollEvents()) {
          val path = event.context() as? Path ?: continue
          val resolvedPath = directory.resolve(path)
          if (Files.isDirectory(resolvedPath)) {
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
              delete(resolvedPath.toFile())
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