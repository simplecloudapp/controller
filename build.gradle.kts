import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
    alias(libs.plugins.sonatypeCentralPortalPublisher)
    `maven-publish`
}

allprojects {
    group = "app.simplecloud.controller"
    version = "0.0.30"

    repositories {
        mavenCentral()
        maven("https://buf.build/gen/maven")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "net.thebugmc.gradle.sonatype-central-portal-publisher")
    apply(plugin = "maven-publish")

    dependencies {
        testImplementation(rootProject.libs.kotlinTest)
        implementation(rootProject.libs.kotlinJvm)
    }

    publishing {
        repositories {
            maven {
                name = "simplecloud"
                url = uri("https://repo.simplecloud.app/snapshots/")
                credentials {
                    username = (project.findProperty("simplecloudUsername") as? String)?: System.getenv("SIMPLECLOUD_USERNAME")
                    password = (project.findProperty("simplecloudPassword") as? String)?: System.getenv("SIMPLECLOUD_PASSWORD")
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }

        publications {
            // Not publish controller-runtime
            if (project.name == "controller-runtime") {
                return@publications
            }

            create<MavenPublication>("mavenJava") {
                from(components["java"])

                val commitHash = System.getenv("COMMIT_HASH")?: return@create
                version = "${project.version}-dev.$commitHash"
            }
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        }
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")
    }

    tasks.test {
        useJUnitPlatform()
    }

    centralPortal {
        name = project.name

        username = project.findProperty("sonatypeUsername") as? String
        password = project.findProperty("sonatypePassword") as? String

        pom {
            name.set("SimpleCloud controller")
            description.set("The heart of SimpleCloud v3")
            url.set("https://github.com/theSimpleCloud/simplecloud-controller")

            developers {
                developer {
                    id.set("fllipeis")
                    email.set("p.eistrach@gmail.com")
                }
                developer {
                    id.set("dayyeeet")
                    email.set("david@cappell.net")
                }
            }
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            scm {
                url.set("https://github.com/theSimpleCloud/simplecloud-controller.git")
                connection.set("git:git@github.com:theSimpleCloud/simplecloud-controller.git")
            }
        }
    }

    signing {
        sign(publishing.publications)
        useGpgCmd()
    }
}