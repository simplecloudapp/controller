package app.simplecloud.controller.api

import app.simplecloud.controller.api.impl.ControllerApiImpl

class ControllerApiSingleton {
    companion object {
        @JvmStatic
        lateinit var instance: ControllerApi
            private set

        fun init() = init(ControllerApiImpl())

        fun init(controllerApi: ControllerApi) {
            instance = controllerApi
        }
    }
}