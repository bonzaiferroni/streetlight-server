package streetlight.server

import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.sse.SSE
import klutch.server.KoinProvider
import klutch.server.configureAuth
import klutch.server.provide
import org.koin.dsl.koinApplication
import streetlight.model.data.StarId
import streetlight.server.model.ClientFacade
import streetlight.server.model.DaoFacade
import streetlight.server.model.Server
import streetlight.server.model.serverModule
import streetlight.server.plugins.*

//val host = "https://streetlight.ing"

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

fun Application.module() {
    // val app = createStreetlight()

    val koin = koinApplication {
        modules(serverModule)
    }.koin

    val provider = KoinProvider(koin)
    val dao = provider.provide<DaoFacade>()
    val client = provider.provide<ClientFacade>()
    val server = Server(provider, dao, client)

    install(Compression) {
        gzip {
            priority = 0.9
//            matchContentType(
//                ContentType.Application.JavaScript
//            )
        }
//        deflate {
//            priority = 1.0
//            matchContentType(
//                ContentType.Text.Any
//            )
//        }
    }

    configureCors()
    configureSerialization()
    configureDatabases()
    configureAuth(provider) { dao.star.readStarPrincipal(StarId(it)) }
    configureWebSockets()
    install(SSE)
    serveApi(server)
    configureLogging()
}
