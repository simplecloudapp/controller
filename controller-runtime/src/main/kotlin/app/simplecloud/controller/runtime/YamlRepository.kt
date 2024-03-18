package app.simplecloud.controller.runtime

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapper
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Collections
import java.util.concurrent.CompletableFuture

abstract class YamlRepository<T>(path: String, private var clazz: Class<T>) : Repository<T>() {

    private lateinit var node: ConfigurationNode
    private lateinit var loader: YamlConfigurationLoader
    private var destination: File = File(path.substring(1, path.length))

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
        addAll(node.getList(clazz) ?: ArrayList())
    }

    override fun delete(element: T): CompletableFuture<Boolean> {
        val index = findIndex(element)
        if (index == -1) {
            return CompletableFuture.completedFuture(false)
        }
        removeAt(index)
        node.set(clazz, this)
        return CompletableFuture.completedFuture(true)
    }

    override fun save(element: T) {
        val index = findIndex(element)
        if (index != -1) {
            removeAt(index)
            add(index, element)
        } else {
            add(element)
        }
        node.setList(clazz, this)
        loader.save(node)
    }

    abstract fun findIndex(element: T): Int
}