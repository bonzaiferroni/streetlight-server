package streetlight.server

import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.sse.SSE
import klutch.server.KoinProvider
import klutch.server.configureAuth
import klutch.server.provide
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import streetlight.server.db.services.StarSessionService
import streetlight.server.model.ClientFacade
import streetlight.server.model.DaoFacade
import streetlight.server.model.Server
import streetlight.server.model.ServerConfig
import streetlight.server.model.ServerScope
import streetlight.server.model.serverModule
import streetlight.server.plugins.*
import java.io.File

//val host = "https://streetlight.ing"

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

/**
 * The Ktor module named in the server config. It builds the services with Koin and starts [streetlightModule];
 * the `streetlight.benchmark` property turns metrics off.
 */
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
        modules(serverModule, module {
            single { ServerConfig(withMetrics = !isBenchmark) }
        })
    }.koin

    val provider = KoinProvider(koin)
    val dao = provider.provide<DaoFacade>()
    val client = provider.provide<ClientFacade>()
    val server = Server(provider, dao, client)
    val session = provider.provide<StarSessionService>()

    streetlightModule(
        server = server,
        session = session,
    )
}

/** Installs the server's plugins and routes, each optional one by the [ServerConfig] that [server] provides. */
fun Application.streetlightModule(
    server: ServerScope,
    session: StarSessionService,
) {
    val config = server.provide<ServerConfig>()

    install(Compression) {
        gzip { priority = 1.0 }
    }

    configureRateLimits(config.withRateLimits)
    if (config.withMetrics) {
        configureMetrics(server)
    }
    configureLogging()
    configureCors()
    configureSerialization()
    if (config.withDatabase) {
        configureDatabases(server)
    }
    configureAuth(session)
    configureWebSockets()
    if (config.withTransit) {
        configureTransit(server)
    }
    install(SSE)
    serveApi(server)
    configureLogging()
}
