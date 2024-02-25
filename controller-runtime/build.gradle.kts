plugins {
    application
}

dependencies {
    api(project(":controller-shared"))
    implementation(rootProject.libs.bundles.log4j)
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

application {
    mainClass.set("app.simplecloud.controller.runtime.launcher.LauncherKt")
}

