package app.simplecloud.controller.runtime.launcher

import app.simplecloud.controller.runtime.ControllerRuntime
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

class ControllerStartCommand: CliktCommand() {

  private val defaultDatabaseUrl = "jdbc:sqlite:database.db"

  val groupPath: Path by option(help = "Path to the group files (groups)", envvar = "GROUPS_PATH").path().default(Path.of("groups"))
  val databaseUrl: String by option(help = "Database URL (${defaultDatabaseUrl})", envvar = "DATABASE_URL").default(defaultDatabaseUrl)

  override fun run() {
    val controllerRuntime = ControllerRuntime(this)
    controllerRuntime.start()
  }
}