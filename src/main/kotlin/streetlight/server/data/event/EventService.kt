package streetlight.server.data.event

import streetlight.model.Event
import streetlight.server.data.ApiService
import streetlight.server.data.location.LocationEntity

class EventService : ApiService() {

    suspend fun create(event: Event): Int = dbQuery {
        val dbLocation = LocationEntity.findById(event.locationId) ?: return@dbQuery -1
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
            val dbLocation = LocationEntity.findById(event.locationId)
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

