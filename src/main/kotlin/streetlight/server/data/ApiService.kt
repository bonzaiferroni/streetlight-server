package streetlight.server.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import streetlight.model.Area
import streetlight.server.data.area.AreaEntity

abstract class ApiService {
    protected suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}