package streetlight.server

import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import klutch.server.configureSecurity
import streetlight.server.model.createStreetlight
import streetlight.server.plugins.*

//val host = "https://streetlight.ing"

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val app = createStreetlight()

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
    serveApi(app)
    configureLogging()
}
