package streetlight.server

import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.sse.SSE
import klutch.server.KoinProvider
import klutch.server.configureAuth
import klutch.server.provide
import org.koin.dsl.koinApplication
import streetlight.server.db.services.StarSessionService
import streetlight.server.model.ClientFacade
import streetlight.server.model.DaoFacade
import streetlight.server.model.Server
import streetlight.server.model.ServerScope
import streetlight.server.model.serverModule
import streetlight.server.plugins.*
import java.io.File

//val host = "https://streetlight.ing"

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

fun Application.module() {
    println(File(".").absolutePath)

    val isBenchmark = environment.config
        .propertyOrNull("streetlight.benchmark")
        ?.getString()
        ?.toBoolean()
        ?: false

    if (isBenchmark) {
        println("--BENCHMARK MODE--")
    }

    val koin = koinApplication {
        modules(serverModule)
    }.koin

    val provider = KoinProvider(koin)
    val dao = provider.provide<DaoFacade>()
    val client = provider.provide<ClientFacade>()
    val server = Server(provider, dao, client)
    val session = provider.provide<StarSessionService>()

    streetlightModule(
        server = server,
        session = session,
        withMetrics = !isBenchmark,
    )
}

fun Application.streetlightModule(
    server: ServerScope,
    session: StarSessionService,
    withMetrics: Boolean = true,
    withDatabase: Boolean = true,
) {
    install(Compression) {
        gzip { priority = 1.0 }
    }

    configureRateLimits()
    if (withMetrics) {
        configureMetrics(server)
    }
    configureLogging()
    configureCors()
    configureSerialization()
    if (withDatabase) {
        configureDatabases(server)
    }
    configureAuth(session)
    configureWebSockets()
    install(SSE)
    serveApi(server)
    configureLogging()
}
