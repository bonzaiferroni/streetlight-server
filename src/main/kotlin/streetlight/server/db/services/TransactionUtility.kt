package streetlight.server.db.services

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.CancellationException
import kampfire.model.Outcome
import kampfire.model.Problem
import klutch.db.DbService
import klutch.utils.logger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

suspend fun <T> tryQuery(
    valueOnError: T,
    maxAttempts: Int = defaultMaxAttempts,
    block: suspend Transaction.() -> T
): T = try {
    suspendTransaction {
        this.maxAttempts = maxAttempts
        block()
    }
} catch(e: CancellationException) {
    throw e
} catch(e: Exception) {
    log.error(e) { "tryQuery exception: ${e.message}" }
    valueOnError
}

private val defaultMaxAttempts: Int = 1

private val log = KotlinLogging.logger(DbService::class)

suspend fun <T> tryOutcome(
    outcomeOnError: Outcome<T> = Problem("There was an internal error"),
    block: suspend () -> Outcome<T>
): Outcome<T> = try {
    block()
} catch(e: CancellationException) {
    throw e
} catch(e: Exception) {
    log.error(e) { "tryOutcome exception: ${e.message}" }
    outcomeOnError
}