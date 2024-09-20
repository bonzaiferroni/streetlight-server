package streetlight.server.db

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.sql.Op
import streetlight.model.core.IdModel

abstract class DataService<Data : IdModel, DataEntity : IntEntity>(
    protected val entity: EntityClass<Int, DataEntity>
) : ApiService() {
    protected abstract suspend fun createEntity(data: Data): (DataEntity.() -> Unit)?
    protected abstract suspend fun updateEntity(data: Data): ((DataEntity) -> Unit)?
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

    suspend fun update(data: Data): Data = dbQuery {
        val update = updateEntity(data) ?: throw IllegalArgumentException("Update not supported")
        val updatedData = entity.findByIdAndUpdate(data.id, update) ?: throw IllegalArgumentException("Not found")
        updatedData.toData()
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
