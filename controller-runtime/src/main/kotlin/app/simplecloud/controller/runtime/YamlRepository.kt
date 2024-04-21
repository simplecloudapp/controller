package app.simplecloud.controller.runtime

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture

abstract class YamlRepository<T>(path: String, private var clazz: Class<T>) : Repository<T>() {

    private lateinit var node: ConfigurationNode
    private lateinit var loader: YamlConfigurationLoader
    private var destination: File = File(path.substring(1, path.length))
    private val list = mutableListOf<T>()

    init {
        if (!destination.exists()) {
            Files.copy(
                YamlRepository::class.java.getResourceAsStream(path)!!,
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
        load()
    }

    final override fun load() {
        val loader = YamlConfigurationLoader.builder()
            .path(destination.toPath())
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(objectMapperFactory())
                }
            }.build()
        this.loader = loader
        node = loader.load()
        list.addAll(node.getList(clazz) ?: ArrayList())
    }

    override fun delete(element: T): CompletableFuture<Boolean> {
        val index = findIndex(element)
        if (index == -1) {
            return CompletableFuture.completedFuture(false)
        }
        list.removeAt(index)
        node.set(clazz, this)
        return CompletableFuture.completedFuture(true)
    }

    override fun save(element: T) {
        val index = findIndex(element)
        if (index != -1) {
            list.removeAt(index)
            list.add(index, element)
        } else {
            list.add(element)
        }
        node.setList(clazz, list)
        loader.save(node)
    }

    abstract fun findIndex(element: T): Int
}