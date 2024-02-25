package app.simplecloud.controller.runtime

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture

abstract class YamlRepository<T>(path: String) : Repository<T>() {

    private lateinit var node: ConfigurationNode
    private var destination: File = File(path.substring(1, path.length))
    init {
        if(!destination.exists()) {
            Files.copy(YamlRepository::class.java.getResourceAsStream(path)!!, destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
    override fun load() {
        val loader = YamlConfigurationLoader.builder()
                .path(destination.toPath())
                .defaultOptions { options ->
                    options.serializers { builder ->
                        builder.registerAnnotatedObjects(objectMapperFactory())
                    }
                }.build()
        node = loader.load()
        addAll(node.get<YamlRepositoryType<T>>()?.items ?: ArrayList())
        forEach {
            println(it)
        }
    }

    override fun delete(element: T): CompletableFuture<Boolean> {
        val index = findIndex(element)
        if(index == -1) {
            return CompletableFuture.completedFuture(false)
        }
        removeAt(index)
        node.set(YamlRepositoryType(this))
        return CompletableFuture.completedFuture(true)
    }

    override fun save(element: T) {
        val index = findIndex(element)
        if(index != -1) {
            removeAt(index)
            add(index, element)
        }else {
            add(element)
        }
        node.set(YamlRepositoryType(this))
    }

    abstract fun findIndex(element: T): Int
}

@ConfigSerializable
data class YamlRepositoryType<T>(var items: List<T>)