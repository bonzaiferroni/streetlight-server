package streetlight.server.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import streetlight.server.model.DaoFacade
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val log = KotlinLogging.logger(TableDaemon::class.simpleName!!)

/** Refreshes the upcoming event counts of locations, cities, and galaxies every fifteen minutes. */
class TableDaemon(
    private val dao: DaoFacade,
) {
    suspend fun start() {
        while (true) {
            try {
                aggregate(Clock.System.now())
            } catch (e: Exception) {
                log.error(e) { "Failed to aggregate tables" }
            }
            delay(15.minutes)
        }
    }

    suspend fun aggregate(now: Instant) {
        dao.location.updateEventCounts(now)
        dao.city.updateEventCounts()
        dao.galaxy.updateEventCounts(now)
    }
}
