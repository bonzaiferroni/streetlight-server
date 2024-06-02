package streetlight.server.data.event

import streetlight.model.Request
import streetlight.server.data.ApiService
import streetlight.server.data.user.PerformanceEntity

class RequestService : ApiService() {

        suspend fun create(request: Request): Int = dbQuery {
            val dbEvent = EventEntity.findById(request.eventId) ?: return@dbQuery -1
            val dbPerformance = PerformanceEntity.findById(request.performanceId) ?: return@dbQuery -1
            RequestEntity.new {
                event = dbEvent
                performance = dbPerformance
                time = request.time
            }.id.value
        }

        suspend fun read(id: Int): Request? {
            return dbQuery {
                RequestEntity.findById(id)?.toRequest()
            }
        }

        suspend fun readAll(): List<Request> {
            return dbQuery {
                RequestEntity.all().map { it.toRequest() }
            }
        }

        suspend fun update(id: Int, request: Request) {
            dbQuery {
                val dbEvent = EventEntity.findById(request.eventId)
                val dbPerformance = PerformanceEntity.findById(request.performanceId)
                RequestEntity.findById(id)?.let {
                    if (dbEvent != null) {
                        it.event = dbEvent
                    }
                    if (dbPerformance != null) {
                        it.performance = dbPerformance
                    }
                    it.time = request.time
                }
            }
        }

        suspend fun delete(id: Int) {
            dbQuery {
                RequestEntity.findById(id)?.delete()
            }
        }
}

fun RequestEntity.toRequest() = Request(
    id.value,
    event.id.value,
    performance.id.value,
    time,
)