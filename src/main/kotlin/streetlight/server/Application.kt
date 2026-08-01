package streetlight.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.sse.SSE
import klutch.server.Authorizer
import klutch.server.KoinProvider
import klutch.server.configureAuth
import klutch.server.provide
import klutch.utils.Log
import org.koin.dsl.koinApplication
import org.slf4j.LoggerFactory
import streetlight.model.data.StarId
import streetlight.server.db.services.StarSessionService
import streetlight.server.model.ClientFacade
import streetlight.server.model.DaoFacade
import streetlight.server.model.Server
import streetlight.server.model.serverModule
import streetlight.server.plugins.*

//val host = "https://streetlight.ing"

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

fun Application.module() {

    val koin = koinApplication {
        modules(serverModule)
    }.koin

    val provider = KoinProvider(koin)
    val dao = provider.provide<DaoFacade>()
    val client = provider.provide<ClientFacade>()
    val server = Server(provider, dao, client)
    val session = provider.provide<StarSessionService>()

    install(Compression) {
        gzip { priority = 1.0 }
    }

    configureRateLimits()
    configureMetrics(server)
    configureLogging()
    configureCors()
    configureSerialization()
    configureDatabases(server)
    configureAuth(session)
    configureWebSockets()
    install(SSE)
    serveApi(server)
    configureLogging()
}
