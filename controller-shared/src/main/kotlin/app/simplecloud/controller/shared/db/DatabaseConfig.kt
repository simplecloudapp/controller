package app.simplecloud.controller.shared.db

import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@ConfigSerializable
data class DatabaseConfig(var driver: String = "jdbc:sqlite", var url: String) {
  companion object {
    const val FILE_NAME = "database.yml"

    fun load(directoryPath: Path): DatabaseConfig? {
      if (System.getenv("DATABASE_URL") != null) {
        return DatabaseConfig(System.getenv("DATABASE_DRIVER") ?: "jdb:sqlite", System.getenv("DATABASE_URL"))
      }
      if (!Files.exists(directoryPath)) {
        Files.createDirectories(directoryPath)
      }

      val path = directoryPath.resolve(FILE_NAME)

      if (!Files.exists(path)) {
        Files.copy(
          DatabaseConfig::class.java.getResourceAsStream("/${FILE_NAME}")!!,
          path,
          StandardCopyOption.REPLACE_EXISTING
        )
      }
      val loader = YamlConfigurationLoader.builder()
        .path(path)
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