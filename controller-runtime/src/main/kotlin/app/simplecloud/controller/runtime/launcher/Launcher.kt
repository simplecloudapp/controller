package app.simplecloud.controller.runtime.launcher

import org.apache.logging.log4j.LogManager


fun main(args: Array<String>) {
    configureLog4j()
    ControllerStartCommand().main(args)
}

fun configureLog4j() {
    val globalExceptionHandlerLogger = LogManager.getLogger("GlobalExceptionHandler")
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        globalExceptionHandlerLogger.error("Uncaught exception in thread ${thread.name}", throwable)
    }
}
