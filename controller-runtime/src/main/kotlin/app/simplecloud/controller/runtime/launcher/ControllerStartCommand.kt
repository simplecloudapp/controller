package app.simplecloud.controller.runtime.launcher

import app.simplecloud.controller.runtime.ControllerRuntime
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

class ControllerStartCommand: CliktCommand() {

  val groupPath: String by option(help = "Path to the group files").default("groups")
  val databaseConfigPath: String by option(help = "Path to the database config file").default("database-config.yml")

  override fun run() {
    val controllerRuntime = ControllerRuntime(this)
    controllerRuntime.start()
  }
}