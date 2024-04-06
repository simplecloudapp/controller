package app.simplecloud.controller.shared.server

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.host.ServerHost
import build.buf.gen.simplecloud.controller.v1.ServerState
import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates

class ServerFactory {

    companion object {
        fun builder(): ServerFactory {
            return ServerFactory()
        }
    }

    private lateinit var group: Group

    fun setGroup(group: Group): ServerFactory {
        this.group = group
        return this
    }

    private var host: ServerHost? = null

    fun setHost(host: ServerHost): ServerFactory {
        this.host = host
        return this
    }

    private var numericalId by Delegates.notNull<Long>()

    fun setNumericalId(numericalId: Long): ServerFactory {
        this.numericalId = numericalId
        return this
    }

    private var port: Long? = null
    fun setPort(port: Long): ServerFactory {
        this.port = port
        return this
    }

    fun build(): Server {
        return Server(
            uniqueId = UUID.randomUUID().toString().replace("-", ""),
            port = port ?: -1,
            type = group.type,
            group = group.name,
            minMemory = group.minMemory,
            maxMemory = group.maxMemory,
            host = host?.id ?: "unknown",
            ip = host?.host ?: "unknown",
            state = ServerState.PREPARING,
            numericalId = numericalId.toInt(),
            playerCount = 0,
            templateId = "",
            properties = mutableMapOf(
                "serverUrl" to group.serverUrl,
                *group.properties.entries.map { it.key to it.value }.toTypedArray()
            ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

}