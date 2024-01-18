plugins {
    application
}

dependencies {
    api(project(":controller-shared"))
    implementation(rootProject.libs.bundles.log4j)
}

application {
    mainClass.set("app.simplecloud.controller.runtime.launcher.LauncherKt")
}