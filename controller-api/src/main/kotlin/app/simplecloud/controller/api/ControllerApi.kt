package app.simplecloud.controller.api

import app.simplecloud.controller.api.impl.ControllerApiImpl
import app.simplecloud.pubsub.PubSubClient

interface ControllerApi {

    /**
     * @return the Controller [GroupApi]
     */
    fun getGroups(): GroupApi

    /**
     * @return the Controller [ServerApi]
     */
    fun getServers(): ServerApi

    /**
     * @return the [PubSubClient] to subscribe to Controller events and send messages
     */
    fun getPubSubClient(): PubSubClient

    companion object {

        /**
         * Creates a new [ControllerApi] instance
         * @return the created [ControllerApi]
         */
        @JvmStatic
        fun create(): ControllerApi {
            val authSecret = System.getenv("CONTROLLER_SECRET")
            return create(authSecret)
        }

        /**
         * Creates a new [ControllerApi] instance
         * @param authSecret the authentication key used by the Controller
         * @return the created [ControllerApi]
         */
        @JvmStatic
        fun create(authSecret: String): ControllerApi {
            return ControllerApiImpl(authSecret)
        }

    }

}