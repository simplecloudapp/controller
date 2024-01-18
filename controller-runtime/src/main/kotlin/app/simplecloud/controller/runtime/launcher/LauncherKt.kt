package app.simplecloud.controller.runtime.launcher

import app.simplecloud.controller.runtime.ControllerRuntime

fun main() {
    val controllerRuntime = ControllerRuntime()
    controllerRuntime.start()
}