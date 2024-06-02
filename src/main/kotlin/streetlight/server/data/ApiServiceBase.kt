package streetlight.server.data

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import streetlight.server.plugins.v1

abstract class ApiServiceBase<Data : Any, DataEntity : IntEntity>(
    val endpoint: String,
    private val entity: EntityClass<Int, DataEntity>
) {
    protected suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    abstract suspend fun createEntity(data: Data): DataEntity.() -> Unit
    abstract fun DataEntity.toData(): Data

    suspend fun create(data: Data): Int = dbQuery {
        entity.new(createEntity(data)).id.value
    }

    suspend fun read(id: Int): Data? = dbQuery {
        entity.findById(id)?.toData()
    }

    suspend fun readAll(): List<Data> = dbQuery {
        entity.all().map { it.toData() }
    }

    suspend fun update(id: Int, data: Data) = dbQuery {
        entity.findById(id)?.let {
            createEntity(data).invoke(it)
        }
    }

    suspend fun delete(id: Int) = dbQuery {
        entity.findById(id)?.delete()
    }
}
