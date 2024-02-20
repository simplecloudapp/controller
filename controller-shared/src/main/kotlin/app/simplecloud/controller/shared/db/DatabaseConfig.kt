package app.simplecloud.controller.shared.db

import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories

@ConfigSerializable
data class DatabaseConfig(var driver: String = "jdbc:sqlite", var url: String) {
    companion object {
        fun load(path: String): DatabaseConfig? {
            if (System.getenv("DATABASE_URL") != null) {
                return DatabaseConfig(System.getenv("DATABASE_DRIVER") ?: "jdb:sqlite", System.getenv("DATABASE_URL"))
            }
            val destination = File(path.substring(1, path.length))
            if (!destination.exists()) {
                Files.createDirectories(destination.toPath())
                destination.createNewFile()
                Files.copy(DatabaseConfig::class.java.getResourceAsStream(path)!!, destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            val loader = YamlConfigurationLoader.builder()
                    .path(destination.toPath())
                    .defaultOptions { options ->
                        options.serializers { builder ->
                            builder.registerAnnotatedObjects(objectMapperFactory())
                        }
                    }.build()
            val node = loader.load()
            val config = node.get<DatabaseConfig>()
            return config
        }
    }

    fun toDatabaseUrl(): String {
        val absoluteUrl = if (url.contains(":")) url else File("").absolutePath + url
        return "$driver:$absoluteUrl"
    }
}