import com.google.protobuf.gradle.*

plugins {
    alias(libs.plugins.protobuf)
}

dependencies {
    api(rootProject.libs.bundles.proto)
}

sourceSets {
    main {
        kotlin {
            srcDirs(
                "build/generated/source/proto/main/grpckt",
                "build/generated/source/proto/main/kotlin",
            )
        }
        java {
            srcDirs(
                "build/generated/source/proto/main/grpc",
                "build/generated/source/proto/main/java",
            )
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.23.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.55.1"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}