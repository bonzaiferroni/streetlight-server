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
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)  // Allow Authorization header
        // anyHost()  // Don't use this in production, specify the exact domain(s)
        allowHost("localhost:3000")
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }
    configureSerialization()
    configureDatabases()
    configureSecurity()
    configureApiRoutes()
    configureHtmlRouting(host)
    configureWebSockets()
}