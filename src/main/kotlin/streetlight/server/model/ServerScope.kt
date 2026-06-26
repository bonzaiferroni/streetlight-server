package streetlight.server.model

import io.ktor.server.routing.Routing
import kabinet.console.globalConsole
import klutch.server.ProviderScope
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

interface ServerScope: DataScope, ProviderScope

interface DataScope: DaoScope, ClientScope

interface DaoScope {
    val dao: DaoFacade

    suspend fun <T> transaction(
        maxAttempts: Int = TRANSACTION_MAX_ATTEMPTS,
        addLogger: Boolean = false,
        block: suspend Transaction.() -> T
    ) = suspendTransaction {
        if (addLogger) {
            addLogger(StdOutSqlLogger)
        }
        this.maxAttempts = maxAttempts
        block()
    }

    fun log(message: String) = daoConsole.log(message)
}

private val daoConsole = globalConsole.getHandle("data")


interface ClientScope {
    val client: ClientFacade
}

interface ApiScope: Routing, ServerScope

class ServerRouting(
    server: ServerScope,
    routing: Routing,
): Routing by routing, ServerScope by server, ApiScope {
}

const val TRANSACTION_MAX_ATTEMPTS = 1