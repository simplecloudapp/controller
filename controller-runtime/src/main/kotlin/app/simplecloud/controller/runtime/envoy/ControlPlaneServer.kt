package app.simplecloud.controller.runtime.envoy

import app.simplecloud.controller.runtime.droplet.DropletRepository
import io.envoyproxy.controlplane.cache.ConfigWatcher
import io.envoyproxy.controlplane.server.V3DiscoveryServer
import io.grpc.ServerBuilder
import org.apache.logging.log4j.LogManager

class ControlPlaneServer(dropletRepository: DropletRepository) {
    private val cache = DropletCache(dropletRepository)
    private val server = V3DiscoveryServer(cache.getCache())
    private val logger = LogManager.getLogger(ControlPlaneServer::class)

    fun register(builder: ServerBuilder<*>) {
        logger.info("Registering envoy control plane server...")
        builder.addService(server.aggregatedDiscoveryServiceImpl)
        logger.info("Registered envoy control plane server.")
    }

    fun getCache(): ConfigWatcher {
        return cache.getCache()
    }
}