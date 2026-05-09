package streetlight.server

import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.sse.SSE
import io.ktor.util.cio.ChannelWriteException
import klutch.server.configureSecurity
import streetlight.server.model.createStreetlight
import streetlight.server.plugins.*

//val host = "https://streetlight.ing"

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val app = createStreetlight()

    install(StatusPages) {
        exception<ChannelWriteException> { call, cause ->
            // common when client disconnects
        }
    }

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
    configureSecurity()
    configureWebSockets()
    install(SSE)
    serveApi(app)
    configureLogging()
}
