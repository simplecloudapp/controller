package app.simplecloud.controller.runtime.envoy

import io.envoyproxy.controlplane.cache.Response
import java.util.*
import java.util.function.Consumer


class SimpleCloudResponseTracker : Consumer<Response> {
    private val responses: LinkedList<Response> = LinkedList<Response>()

    override fun accept(response: Response) {
        responses.add(response)
    }
}