package streetlight.server

import streetlight.server.plugins.*
import io.ktor.server.application.*

//val host = "https://streetlight.ing"
val host = "http://192.168.1.122:8080"

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureSecurity()
    configureApiRoutes()
    configureHtmlRouting(host)
    configureWebSockets()
}