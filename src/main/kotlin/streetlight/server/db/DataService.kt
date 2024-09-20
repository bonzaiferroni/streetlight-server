package streetlight.server.db

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.sql.Op
import streetlight.model.core.IdModel

abstract class DataService<Data : IdModel, DataEntity : IntEntity>(
    private val entity: EntityClass<Int, DataEntity>,
    private val fromObj: DataEntity.(Data) -> Unit,
    private val toObj: DataEntity.() -> Data,
) : ApiService() {
    open fun getSearchOp(search: String): Op<Boolean> = Op.TRUE

    suspend fun create(data: Data): Int = dbQuery {
        entity.new { fromObj(data) }.id.value
    }

    suspend fun read(id: Int): Data? = dbQuery {
        entity.findById(id)?.toObj()
    }

    suspend fun readAll(): List<Data> = dbQuery {
        entity.all().map { it.toObj() }
    }

    suspend fun update(data: Data): Data = dbQuery {
        val updatedData = entity.findByIdAndUpdate(data.id) { fromObj(it, data) }
            ?: throw IllegalArgumentException("Not found")
        updatedData.toObj()
    }

    suspend fun delete(id: Int) = dbQuery {
        entity.findById(id)?.delete()
    }

    suspend fun search(op: Op<Boolean>, limit: Int): List<Data> {
        return dbQuery {
            entity.find(op)
                .limit(limit)
                .map { it.toObj() }
        }
    }
}
