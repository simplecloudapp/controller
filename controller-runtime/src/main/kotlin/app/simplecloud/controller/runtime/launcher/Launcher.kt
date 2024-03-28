package app.simplecloud.controller.runtime.launcher

import app.simplecloud.controller.runtime.ControllerRuntime


fun main(args: Array<String>) {
    val arguments: MutableMap<String, String> = HashMap()
    for (arg in args) {
        if (arg.startsWith("--") && arg.contains("=")) {
            val parts = arg.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val key = parts[0].substring(2)
            val value = parts[1]
            arguments[key] = value
        }
    }
    val controllerRuntime = ControllerRuntime()
    controllerRuntime.start(arguments)
}