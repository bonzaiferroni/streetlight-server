package streetlight.server.data.user

import streetlight.model.Performance
import streetlight.server.data.ApiService

class PerformanceService : ApiService() {

    suspend fun create(performance: Performance): Int = dbQuery {
        val user = UserEntity.findById(performance.userId) ?: return@dbQuery -1
        PerformanceEntity.new {
            this.user = user
            name = performance.name
        }.id.value
    }

    suspend fun read(id: Int): Performance? {
        return dbQuery {
            PerformanceEntity.findById(id)?.toPerformance()
        }
    }

    suspend fun readAll(): List<Performance> {
        return dbQuery {
            PerformanceEntity.all().map { it.toPerformance() }
        }
    }

    suspend fun update(id: Int, performance: Performance) {
        dbQuery {
            val user = UserEntity.findById(performance.userId)
            PerformanceEntity.findById(id)?.let {
                if (user != null) {
                    it.user = user
                }
                it.name = performance.name
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            PerformanceEntity.findById(id)?.delete()
        }
    }
}

fun PerformanceEntity.toPerformance() = Performance(
    this.id.value,
    this.user.id.value,
    this.name
)