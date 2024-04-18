package app.simplecloud.controller.runtime.launcher

import app.simplecloud.controller.runtime.ControllerRuntime
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

class ControllerStartCommand : CliktCommand() {

    private val defaultDatabaseUrl = "jdbc:sqlite:database.db"

    val groupPath: Path by option(help = "Path to the group files (default: groups)", envvar = "GROUPS_PATH")
        .path()
        .default(Path.of("groups"))
    val databaseUrl: String by option(help = "Database URL (default: ${defaultDatabaseUrl})", envvar = "DATABASE_URL")
        .default(defaultDatabaseUrl)

    val grpcHost: String by option(help = "Grpc host (default: localhost)", envvar = "GRPC_HOST").default("localhost")
    val grpcPort: Int by option(help = "Grpc port (default: 5816)", envvar = "GRPC_PORT").int().default(5816)

    val velocitySecretPath: Path by option(help = "Path to the velocity secret (default: forwarding.secret)", envvar = "VELOCITY_SECRET_PATH")
        .path()
        .default(Path.of("forwarding.secret"))

    override fun run() {
        val controllerRuntime = ControllerRuntime(this)
        controllerRuntime.start()
    }
}