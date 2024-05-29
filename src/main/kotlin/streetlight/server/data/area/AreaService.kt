package streetlight.server.data.area

import streetlight.model.Area
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import streetlight.server.data.ApiService


class AreaService(database: Database) : ApiService(database, AreaTable) {

    suspend fun create(area: Area): Int = dbQuery {
        AreaEntity.new {
            name = area.name
        }.id.value
    }

    suspend fun read(id: Int): Area? {
        return dbQuery {
            AreaEntity.findById(id)
                ?.let {
                    Area(
                        it.id.value,
                        it.name,
                    )
                }
        }
    }

    suspend fun readAll(): List<Area> {
        return dbQuery {
            AreaEntity.all().map {
                Area(
                    it.id.value,
                    it.name,
                )
            }
        }
    }

    suspend fun update(id: Int, area: Area) {
        dbQuery {
            AreaEntity.findById(id)?.let {
                it.name = area.name
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            AreaEntity.findById(id)?.delete()
        }
    }
}

