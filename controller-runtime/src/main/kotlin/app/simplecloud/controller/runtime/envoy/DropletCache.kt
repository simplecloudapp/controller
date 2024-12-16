package app.simplecloud.controller.runtime.envoy

import app.simplecloud.controller.runtime.droplet.DropletRepository
import com.google.protobuf.Any
import com.google.protobuf.Duration
import com.google.protobuf.UInt32Value
import io.envoyproxy.controlplane.cache.ConfigWatcher
import io.envoyproxy.controlplane.cache.Resources
import io.envoyproxy.controlplane.cache.Watch
import io.envoyproxy.controlplane.cache.XdsRequest
import io.envoyproxy.controlplane.cache.v3.SimpleCache
import io.envoyproxy.controlplane.cache.v3.Snapshot
import io.envoyproxy.envoy.config.cluster.v3.Cluster
import io.envoyproxy.envoy.config.cluster.v3.Cluster.EdsClusterConfig
import io.envoyproxy.envoy.config.core.v3.*
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment
import io.envoyproxy.envoy.config.endpoint.v3.Endpoint
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints
import io.envoyproxy.envoy.config.listener.v3.Filter
import io.envoyproxy.envoy.config.listener.v3.FilterChain
import io.envoyproxy.envoy.config.listener.v3.Listener
import io.envoyproxy.envoy.config.route.v3.*
import io.envoyproxy.envoy.extensions.filters.http.connect_grpc_bridge.v3.FilterConfig
import io.envoyproxy.envoy.extensions.filters.http.grpc_web.v3.GrpcWeb
import io.envoyproxy.envoy.extensions.filters.http.router.v3.Router
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter
import io.envoyproxy.envoy.extensions.upstreams.http.v3.HttpProtocolOptions
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class DropletCache(private val dropletRepository: DropletRepository) {
    private val cache = SimpleCache(SimpleCloudNodeGroup())
    private var watch: Watch = cache.createWatch(
        true,
        XdsRequest.create(
            DiscoveryRequest.newBuilder().setNode(Node.getDefaultInstance())
                .setTypeUrl(Resources.V3.ENDPOINT_TYPE_URL).addResourceNames("none").build()
        ),
        Collections.emptySet(),
        SimpleCloudResponseTracker()
    )

    init {
        CoroutineScope(Dispatchers.IO).launch {
            update()
        }
    }

    suspend fun update() {
        //Gets the simplecloud group, if not found throws an error
        val group = cache.groups().firstOrNull() ?: throw IllegalArgumentException("Group not found")
        cache.setSnapshot(
            group,
            Snapshot.create(
                createClusters(),
                listOf(),
                createListeners(),
                listOf(),
                listOf(),
                UUID.randomUUID().toString()
            )
        )
    }

    fun stopWatch() {
        watch.cancel()
    }

    private suspend fun createListeners(): List<Listener> {
        return dropletRepository.getAll().map {
            Listener.newBuilder().setName("${it.type}-${it.id}").setAddress(
                Address.newBuilder().setSocketAddress(
                    SocketAddress.newBuilder().setProtocol(SocketAddress.Protocol.TCP).setAddress("0.0.0.0")
                        .setPortValue(it.envoyPort)
                )
            ).setDefaultFilterChain(listenerFilterChain("${it.type}-${it.id}")).build()
        }
    }

    private suspend fun createClusters(): List<Cluster> {
        return dropletRepository.getAll().map {
            Cluster.newBuilder().setName("${it.type}-${it.id}").setConnectTimeout(Duration.newBuilder().setSeconds(5))
                .setType(Cluster.DiscoveryType.EDS)
                .setEdsClusterConfig(
                    EdsClusterConfig.newBuilder()
                        .setEdsConfig(ConfigSource.newBuilder().setAds(AggregatedConfigSource.getDefaultInstance()))
                )
                .setLbPolicy(Cluster.LbPolicy.ROUND_ROBIN)
                .setLoadAssignment(
                    ClusterLoadAssignment.newBuilder().setClusterName("${it.type}-${it.id}")
                        .addEndpoints(
                            LocalityLbEndpoints.newBuilder()
                                .addLbEndpoints(
                                    LbEndpoint.newBuilder().setEndpoint(
                                        Endpoint.newBuilder()
                                            .setAddress(
                                                Address.newBuilder().setSocketAddress(
                                                    SocketAddress.newBuilder().setPortValue(it.port).setAddress(it.host)
                                                )
                                            )
                                    )
                                )
                        )
                ).putTypedExtensionProtocolOptions(
                    "envoy.extensions.upstreams.http.v3.HttpProtocolOptions", Any.pack(
                        HttpProtocolOptions.newBuilder().setExplicitHttpConfig(
                            HttpProtocolOptions.ExplicitHttpConfig.newBuilder().setHttp2ProtocolOptions(
                                Http2ProtocolOptions.newBuilder().setMaxConcurrentStreams(
                                    UInt32Value.of(100)
                                )
                            )
                        ).build()
                    )
                )
                .build()
        }
    }

    private fun listenerFilterChain(cluster: String): FilterChain.Builder {
        return FilterChain.newBuilder()
            .addFilters(
                Filter.newBuilder().setName("envoy.filters.network.http_connection_manager")
                    .setTypedConfig(
                        Any.pack(
                            HttpConnectionManager.newBuilder().setStatPrefix("ingress_http")
                                .setCodecType(HttpConnectionManager.CodecType.AUTO)
                                .setRouteConfig(
                                    RouteConfiguration.newBuilder().setName("local_route")
                                        .addVirtualHosts(
                                            VirtualHost.newBuilder().setName("local_service").addDomains("*")
                                                .addRoutes(
                                                    Route.newBuilder().setRoute(
                                                        RouteAction.newBuilder().setCluster(cluster)
                                                            .setTimeout(Duration.newBuilder().setSeconds(0).setNanos(0))
                                                    ).setMatch(RouteMatch.newBuilder().setPrefix("/"))
                                                )
                                        )
                                ).addHttpFilters(
                                    HttpFilter.newBuilder().setName("envoy.filters.http.connect_grpc_bridge")
                                        .setTypedConfig(Any.pack(FilterConfig.getDefaultInstance()))
                                ).addHttpFilters(
                                    HttpFilter.newBuilder().setName("envoy.filters.http.grpc_web")
                                        .setTypedConfig(Any.pack(GrpcWeb.getDefaultInstance()))
                                ).addHttpFilters(
                                    HttpFilter.newBuilder().setName("envoy.filters.http.router")
                                        .setTypedConfig(Any.pack(Router.getDefaultInstance()))
                                )

                                .build()
                        )
                    )
            )
    }

    fun getCache(): ConfigWatcher {
        return cache
    }

}