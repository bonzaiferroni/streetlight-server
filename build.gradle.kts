import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "streetlight.server"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    implementation(libs.kotlinx.datetime)

    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)
    implementation(libs.ktor.serialization.kotlinx.cbor)
    implementation(libs.ktor.server.content.negotiation.jvm)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation("org.postgresql:postgresql:42.7.3")
    implementation(libs.exposed.migration.core)
    implementation(libs.exposed.migration.jdbc)

    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.ktor.server.auth.jwt.jvm)
    implementation(libs.ktor.server.netty.jvm)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.status.pages)

    implementation(libs.ktor.server.metrics.micrometer)
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.3")

    implementation(libs.logback.classic)
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    implementation(project(":model"))
    implementation(project(":kabinet"))
    implementation(project(":klutch"))
    implementation(project(":web"))
    implementation(project(":agent"))
    implementation(project(":koala"))

    implementation("com.sksamuel.scrimage:scrimage-core:4.3.6")
    implementation("com.sksamuel.scrimage:scrimage-webp:4.3.6")
    implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
    implementation("com.google.transit:gtfs-realtime-bindings:0.0.4")
    implementation(libs.ktor.serialization.kotlinx.cbor)

    implementation(platform("aws.sdk.kotlin:bom:1.6.52"))
    implementation("aws.sdk.kotlin:s3")

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}

ktor {
    fatJar {
        archiveFileName.set("streetlight-server.jar")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}