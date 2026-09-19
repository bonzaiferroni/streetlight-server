import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "streetlight.server"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.cio.EngineMain")
}

dependencies {
    implementation(libs.kotlinx.datetime)

    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)
    implementation(libs.ktor.serialization.kotlinx.cbor)
    implementation(libs.ktor.server.content.negotiation.jvm)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    api(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.postgresql)
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
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.rate.limit)

    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)

    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)

    implementation(project(":model"))
    implementation(project(":kabinet"))
    implementation(project(":klutch"))
    implementation(project(":web"))
    implementation(project(":agent"))
    implementation(project(":koala"))

    implementation(libs.scrimage.core)
    implementation(libs.scrimage.webp)
    implementation(libs.fleeksoft.ksoup)
    implementation(libs.gtfs.realtime.bindings)

    implementation(platform(libs.aws.sdk.bom))
    implementation(libs.aws.sdk.s3)

    api(project.dependencies.platform(libs.koin.bom))
    api(libs.koin.core)

    implementation(libs.flexmark.html2md.converter)
    implementation(libs.timeshape)

    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.playwright)
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
    useJUnitPlatform {
        excludeTags("e2e")
    }
}

tasks.register<Test>("e2eTest") {
    group = "verification"
    description = "Runs browser tests against a real server and a real database"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("e2e")
    }
    // the browser loads the compiled bundle, so it must exist before the server starts
    dependsOn(":web:jsBrowserDevelopmentWebpack")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.register<JavaExec>("generateMigration") {
    group = "database"
    description = "Diffs the table definitions against a shadow database and writes the next migration"
    mainClass.set("streetlight.server.tools.GenerateMigrationKt")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = projectDir
    doFirst {
        val name = project.findProperty("migration")?.toString()
            ?: error("pass -Pmigration=<snake_case_description>")
        args = listOf(name)
    }
}
