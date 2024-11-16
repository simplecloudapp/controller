package app.simplecloud.controller.runtime.launcher

import app.simplecloud.controller.runtime.ControllerRuntime
import app.simplecloud.controller.shared.secret.AuthFileSecretFactory
import app.simplecloud.metrics.internal.api.MetricsCollector
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.clikt.sources.ValueSource
import java.io.File
import java.nio.file.Path

class ControllerStartCommand(
    private val metricsCollector: MetricsCollector?
)  : CliktCommand() {

    init {
        context {
            valueSource = PropertiesValueSource.from(File("controller.properties"), false, ValueSource.envvarKey())
        }
    }

    private val defaultDatabaseUrl = "jdbc:sqlite:database.db"

    val groupPath: Path by option(help = "Path to the group files (default: groups)", envvar = "GROUPS_PATH")
        .path()
        .default(Path.of("groups"))
    val databaseUrl: String by option(help = "Database URL (default: ${defaultDatabaseUrl})", envvar = "DATABASE_URL")
        .default(defaultDatabaseUrl)

    val grpcHost: String by option(help = "Grpc host (default: localhost)", envvar = "GRPC_HOST").default("localhost")
    val grpcPort: Int by option(help = "Grpc port (default: 5816)", envvar = "GRPC_PORT").int().default(5816)

    val pubSubGrpcPort: Int by option(help = "PubSub Grpc port (default: 5817)", envvar = "PUBSUB_GRPC_PORT").int()
        .default(5817)

    private val authSecretPath: Path by option(
        help = "Path to auth secret file (default: .auth.secret)",
        envvar = "AUTH_SECRET_PATH"
    )
        .path()
        .default(Path.of(".secrets", "auth.secret"))

    val authSecret: String by option(help = "Auth secret", envvar = "AUTH_SECRET_KEY")
        .defaultLazy { AuthFileSecretFactory.loadOrCreate(authSecretPath) }

    private val trackMetrics: Boolean by option(help = "Track metrics", envvar = "TRACK_METRICS")
        .boolean()
        .default(true)

    override fun run() {
        if (trackMetrics) {
            metricsCollector?.start()
        }

        val controllerRuntime = ControllerRuntime(this)
        controllerRuntime.start()
    }
}