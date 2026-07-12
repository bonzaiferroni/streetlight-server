package streetlight.server.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import kampfire.model.Identity
import org.slf4j.MDC
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

fun Application.configureLogging() {
    intercept(ApplicationCallPipeline.Setup) {
        val requestId = Uuid.random().toString()
        MDC.put("requestId", requestId)
        MDC.put("method", call.request.httpMethod.value)
        MDC.put("path", call.request.path())

        try {
            proceed()
        } finally {
            MDC.clear()
        }
    }

    intercept(ApplicationCallPipeline.Plugins) {
        val principal = call.principal<Identity>()
        principal?.let { MDC.put("userId", it.callerId.toString()) }
        proceed()
    }
}

fun KotlinLogging.logger(type: KClass<*>) = logger(type.simpleName!!)

//fun Application.configureLogging() {
//    Log.initialize(environment.log)
//}
//
//object Log {
//    private var logger: Logger? = null
//
//    fun initialize(logger: Logger) {
//        this.logger = logger
//    }
//
//    fun logInfo(message: String) {
//        logger?.info(message)
//    }
//
//    fun logError(message: String) {
//        logger?.error(message)
//    }
//
//    fun logWarn(message: String) {
//        logger?.warn(message)
//    }
//
//    fun logDebug(message: String) {
//        logger?.debug(message)
//    }
//
//    fun logTrace(message: String) {
//        logger?.trace(message)
//    }
//}