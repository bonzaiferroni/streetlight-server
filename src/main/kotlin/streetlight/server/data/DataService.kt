package streetlight.server.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

abstract class DataService<Data : Any, DataEntity : IntEntity>(
    val endpoint: String,
    protected val entity: EntityClass<Int, DataEntity>
) : ApiService() {
    protected abstract suspend fun createEntity(data: Data): (DataEntity.() -> Unit)?
    protected abstract fun DataEntity.toData(): Data
    open fun getSearchOp(search: String): Op<Boolean> = Op.TRUE

    suspend fun create(data: Data): Int = dbQuery {
        val creation = createEntity(data) ?: return@dbQuery -1
        entity.new(creation).id.value
    }

    suspend fun read(id: Int): Data? = dbQuery {
        entity.findById(id)?.toData()
    }

    suspend fun readAll(): List<Data> = dbQuery {
        entity.all().map { it.toData() }
    }

    suspend fun update(id: Int, data: Data) = dbQuery {
        val creation = createEntity(data) ?: return@dbQuery
        entity.findById(id)?.let {
            creation
        }
    }

    suspend fun delete(id: Int) = dbQuery {
        entity.findById(id)?.delete()
    }

    suspend fun search(op: Op<Boolean>, limit: Int): List<Data> {
        return dbQuery {
            entity.find(op)
                .limit(limit)
                .map { it.toData() }
        }
    }
}
