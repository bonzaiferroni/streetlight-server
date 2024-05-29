package streetlight.server.data.event

import streetlight.model.Event
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import streetlight.server.data.location.LocationService
import streetlight.server.data.location.LocationTable
import streetlight.server.data.location.Locations

class EventService(database: Database) {

    init {
        transaction(database) {
            SchemaUtils.create(EventTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(event: Event): Int = dbQuery {
        val dbLocation = Locations.findById(event.locationId) ?: return@dbQuery -1
        EventEntity.new {
            location = dbLocation
            timeStart = event.startTime
            timeEnd = event.endTime
        }.id.value
    }

    suspend fun read(id: Int): Event? {
        return dbQuery {
            EventEntity.findById(id)
                ?.let {
                    Event(
                        it.id.value,
                        it.location.id.value,
                        it.timeStart,
                        it.timeEnd,
                    )
                }
        }
    }

    suspend fun readAll(): List<Event> {
        return dbQuery {
            EventEntity.all().map {
                Event(
                    it.id.value,
                    it.location.id.value,
                    it.timeStart,
                    it.timeEnd,
                )
            }
        }
    }

    suspend fun update(id: Int, event: Event) {
        dbQuery {
            val dbLocation = Locations.findById(event.locationId)
            EventEntity.findById(id)?.let {
                if (dbLocation != null) {
                    it.location = dbLocation
                }
                it.timeStart = event.startTime
                it.timeEnd = event.endTime
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            EventEntity.findById(id)?.delete()
        }
    }
}

