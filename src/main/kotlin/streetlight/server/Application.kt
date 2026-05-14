package streetlight.server

import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.sse.SSE
import klutch.server.ServerContext
import klutch.server.configureAuth
import org.koin.dsl.koinApplication
import streetlight.model.data.StarId
import streetlight.server.model.DaoFacade
import streetlight.server.model.serverModule
import streetlight.server.plugins.*

//val host = "https://streetlight.ing"

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

fun Application.module() {
    // val app = createStreetlight()

    val koin = koinApplication {
        modules(serverModule)
    }.koin

    val server = ServerContext(koin)
    val dao = server.get<DaoFacade>()

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
    configureAuth(server) { dao.star.readStarPrincipal(StarId(it)) }
    configureWebSockets()
    install(SSE)
    serveApi(server)
    configureLogging()
}
