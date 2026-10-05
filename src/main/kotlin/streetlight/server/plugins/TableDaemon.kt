package streetlight.server.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import kampfire.model.distanceTo
import kampfire.model.kilometers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import streetlight.server.model.DaoFacade
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val log = KotlinLogging.logger(TableDaemon::class.simpleName!!)

/**
 * Refreshes the upcoming event counts of locations, cities, and galaxies every fifteen minutes, and the map priorities
 * of locations daily.
 */
class TableDaemon(
    private val dao: DaoFacade,
) {
    suspend fun start(): Unit = coroutineScope {
        launch {
            while (true) {
                try {
                    aggregate(Clock.System.now())
                } catch (e: Exception) {
                    log.error(e) { "Failed to aggregate tables" }
                }
                delay(15.minutes)
            }
        }
        launch {
            while (true) {
                try {
                    updateMapPriorities(Clock.System.now())
                } catch (e: Exception) {
                    log.error(e) { "Failed to update map priorities" }
                }
                delay(1.days)
            }
        }
    }

    suspend fun aggregate(now: Instant) {
        dao.location.updateEventCounts(now)
        dao.city.updateEventCounts()
        dao.galaxy.updateEventCounts(now)
    }

    /** Scores each location by the nearness of its next event and the fewness of its neighbors. */
    suspend fun updateMapPriorities(now: Instant) {
        val inputs = dao.location.readPriorityInputs(now)
        val eventInputs = inputs.filter { it.nextEventAt != null }

        inputs.forEach { input ->
            val nearbyCount = 1 + eventInputs.count {
                it.locationId != input.locationId && it.geoPoint.distanceTo(input.geoPoint) <= VicinityRadius
            }
            val eventPriority = input.nextEventAt?.let { 1.0 / (1 + (it - now).inWholeDays) } ?: 0.0
            val vicinityPriority = 1.0 / nearbyCount
            val mapPriority = EventWeight * eventPriority + VicinityWeight * vicinityPriority
            dao.location.updateMapPriority(input.locationId, mapPriority)
        }
    }
}

/** The weight of a location's next event in its map priority. */
private const val EventWeight = 1.0

/** The weight of a location's few neighbors with events in its map priority. */
private const val VicinityWeight = 1.0

/** The distance within which another location counts as a neighbor, about the cluster radius at a city's initial zoom. */
private val VicinityRadius = 3.kilometers
