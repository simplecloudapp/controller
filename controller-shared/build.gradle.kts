import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.protobuf)
    alias(libs.plugins.jooqCodegen)
}

dependencies {
    api(rootProject.libs.bundles.proto)
    api(rootProject.libs.bundles.jooq)
    api(rootProject.libs.bundles.configurate)
    api(rootProject.libs.sqliteJdbc)
    jooqCodegen(rootProject.libs.jooqMetaExtensions)
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
                "build/generated/source/db/main/java",
            )
        }
        resources {
            srcDirs(
                "src/main/db"
            )
        }
    }
}

protobuf {
    protoc {
        artifact = libs.protobufProtoc.get().toString()
    }
    plugins {
        id("grpc") {
            artifact = libs.protobufGenGrpc.get().toString()
        }
        id("grpckt") {
            artifact = libs.protobufGenGrpcKotlin.get().toString()
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

tasks.named("compileKotlin") {
    dependsOn(tasks.jooqCodegen)
}

jooq {
    configuration {
        generator {
            target {
                directory = "build/generated/source/db/main/java"
                packageName = "app.simplecloud.controller.shared.db"
            }
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                properties {
                    // Specify the location of your SQL script.
                    // You may use ant-style file matching, e.g. /path/**/to/*.sql
                    //
                    // Where:
                    // - ** matches any directory subtree
                    // - * matches any number of characters in a directory / file name
                    // - ? matches a single character in a directory / file name
                    property {
                        key = "scripts"
                        value = "src/main/db/schema.sql"
                    }

                    // The sort order of the scripts within a directory, where:
                    //
                    // - semantic: sorts versions, e.g. v-3.10.0 is after v-3.9.0 (default)
                    // - alphanumeric: sorts strings, e.g. v-3.10.0 is before v-3.9.0
                    // - flyway: sorts files the same way as flyway does
                    // - none: doesn't sort directory contents after fetching them from the directory
                    property {
                        key = "sort"
                        value = "semantic"
                    }

                    // The default schema for unqualified objects:
                    //
                    // - public: all unqualified objects are located in the PUBLIC (upper case) schema
                    // - none: all unqualified objects are located in the default schema (default)
                    //
                    // This configuration can be overridden with the schema mapping feature
                    property {
                        key = "unqualifiedSchema"
                        value = "none"
                    }

                    // The default name case for unquoted objects:
                    //
                    // - as_is: unquoted object names are kept unquoted
                    // - upper: unquoted object names are turned into upper case (most databases)
                    // - lower: unquoted object names are turned into lower case (e.g. PostgreSQL)
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                }
            }
        }
    }
}

