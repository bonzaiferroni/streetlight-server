package streetlight.server

import io.ktor.http.*
import streetlight.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

//val host = "https://streetlight.ing"
val host = "http://192.168.1.122:8080"

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureCors()
    configureSerialization()
    configureDatabases()
    configureSecurity()
    configureApiRoutes()
    configureHtmlRouting(host)
    configureWebSockets()
}