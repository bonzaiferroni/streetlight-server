package streetlight.server.utilities

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import streetlight.model.Area
import streetlight.model.Event
import streetlight.model.Location
import streetlight.model.Performance
import streetlight.model.Request
import streetlight.model.User
import streetlight.server.db.services.AreaService
import streetlight.server.db.services.EventService
import streetlight.server.db.services.RequestService
import streetlight.server.db.services.LocationService
import streetlight.server.db.services.PerformanceService
import streetlight.server.db.services.UserService
import java.io.File
import kotlin.reflect.KClass

object DbBackup {

    suspend fun create() {
        val areas = AreaService().readAll()
        val events = EventService().readAll()
        val locations = LocationService().readAll()
        val performances = PerformanceService().readAll()
        val requests = RequestService().readAll()
        val users = UserService().readAll()

        val backup = DataBackup(areas, events, locations, performances, requests, users)
        val json = Json.encodeToString(backup)
        File("backup.json").writeText(json)
    }

    suspend fun restore() {
        val file = File("backup.json")
        if (!file.exists()) {
            println("backup not found")
            return
        }
        val json = file.readText()
        val backup = Json.decodeFromString<DataBackup>(json)

        backup.users.forEach {
            val userId = UserService().create(it)
            IdMap.setNewId(User::class, it.id, userId)
        }
        backup.areas.forEach {
            val areaId = AreaService().create(it)
            IdMap.setNewId(Area::class, it.id, areaId)
        }
        backup.locations.forEach {
            val areaId = IdMap.getNewId(Area::class, it.areaId) ?: throw IllegalStateException(
                "Area not found"
            )
            val locationId = LocationService().create(it.copy(areaId = areaId))
            IdMap.setNewId(Location::class, it.id, locationId)
        }
        backup.events.forEach {
            val locationId = IdMap.getNewId(Location::class, it.locationId)
                ?: throw IllegalStateException("Location not found")
            val userId = IdMap.getNewId(User::class, it.userId)
                ?: throw IllegalStateException("User not found")
            val eventId = EventService().create(it.copy(locationId = locationId, userId = userId))
            IdMap.setNewId(Event::class, it.id, eventId)
        }
        backup.performances.forEach {
            val userId = IdMap.getNewId(User::class, it.userId)
                ?: throw IllegalStateException("User not found")
            val performanceId = PerformanceService().create(it.copy(userId = userId))
            IdMap.setNewId(Performance::class, it.id, performanceId)
        }
        backup.requests.forEach {
            val eventId = IdMap.getNewId(Event::class, it.eventId)
                ?: throw IllegalStateException("Event not found")
            val performanceId = IdMap.getNewId(Performance::class, it.performanceId)
                ?: throw IllegalStateException("Performance not found")
            val requestId = RequestService()
                .create(it.copy(eventId = eventId, performanceId = performanceId))
            IdMap.setNewId(Request::class, it.id, requestId)
        }
    }
}

object IdMap {
    private val map = mutableMapOf<KClass<*>, MutableMap<Int, Int>>()

    fun getNewId(type: KClass<*>, previousId: Int): Int? {
        return map[type]?.get(previousId)
    }

    fun setNewId(type: KClass<*>, previousId: Int, newId: Int) {
        val childMap = map.getOrPut(type) { mutableMapOf() }
        childMap[previousId] = newId
    }
}

@Serializable
data class DataBackup(
    val areas: List<Area>,
    val events: List<Event>,
    val locations: List<Location>,
    val performances: List<Performance>,
    val requests: List<Request>,
    val users: List<User>,
)