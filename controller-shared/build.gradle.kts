dependencies {
    api(rootProject.libs.bundles.proto)
    api(rootProject.libs.simplecloud.pubsub)
    api(rootProject.libs.bundles.configurate)
    api(rootProject.libs.clikt)
    api(rootProject.libs.kotlin.coroutines)
    api(libs.bundles.ktor)
    api(libs.nimbus.jose.jwt)
    implementation(libs.gson)
}
