plugins {
    application
}

dependencies {
    api(project(":controller-shared"))
    api(rootProject.libs.kotlinCoroutines)
    implementation(rootProject.libs.bundles.log4j)
    implementation(rootProject.libs.clikt)
}

application {
    mainClass.set("app.simplecloud.controller.runtime.launcher.LauncherKt")
}

