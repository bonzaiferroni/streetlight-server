package streetlight.server

import io.ktor.server.application.ApplicationCall

fun ApplicationCall.getIdOrThrow(): Int {
    return this.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
}

fun ApplicationCall.logInfo(message: String) {
    this.application.environment.log.info(message)
}

fun ApplicationCall.logError(message: String) {
    this.application.environment.log.error(message)
}

fun ApplicationCall.logWarn(message: String) {
    this.application.environment.log.warn(message)
}

fun ApplicationCall.logDebug(message: String) {
    this.application.environment.log.debug(message)
}

fun ApplicationCall.logTrace(message: String) {
    this.application.environment.log.trace(message)
}