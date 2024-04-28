package app.simplecloud.controller.api

import app.simplecloud.controller.api.impl.ControllerApiImpl

interface ControllerApi {

    fun getGroups(): GroupApi

    fun getServers(): ServerApi

    companion object {

        @JvmStatic
        fun create(): ControllerApi {
            val authSecret = System.getenv("CONTROLLER_SECRET")
            return create(authSecret)
        }

        @JvmStatic
        fun create(authSecret: String): ControllerApi {
            return ControllerApiImpl(authSecret)
        }

    }

}