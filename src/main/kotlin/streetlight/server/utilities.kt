package streetlight.server

import io.ktor.server.application.ApplicationCall

fun ApplicationCall.getIdOrThrow(): Int {
    return this.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
}

fun ApplicationCall.logInfo(message: String) {
    this.application.environment.log.info(message)
}