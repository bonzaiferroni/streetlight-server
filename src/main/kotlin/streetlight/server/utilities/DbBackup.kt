package streetlight.server.utilities

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import streetlight.model.core.*
import streetlight.server.db.core.VariableStore
import streetlight.server.db.services.*
import streetlight.server.models.User
import java.io.File
import kotlin.reflect.KClass

object DbBackup {

    suspend fun create() {
        val areas = AreaService().readAll()
        val events = EventService().readAll()
        val locations = LocationService().readAll()
        val songs = SongService().readAll()
        val requests = RequestService().readAll()
        val users = UserService().readAll()

        val backup = DataBackup(areas, events, locations, songs, requests, users)
        val json = Json.encodeToString(backup)
        File("backup.json").writeText(json)
    }

    suspend fun restore() {
        suspend fun createAdmin() {
            println("creating admin")
            val admin = VariableStore().admin
            UserService().create(admin)
        }

        val file = File("backup.json")
        if (!file.exists()) {
            println("backup not found")
            createAdmin()
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
            val areaId = it.areaId?.let { id -> IdMap.getNewId(Area::class, id) }
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
        backup.songs.forEach {
            val userId = IdMap.getNewId(User::class, it.userId)
                ?: throw IllegalStateException("User not found")
            val songId = SongService().create(it.copy(userId = userId))
            IdMap.setNewId(Song::class, it.id, songId)
        }
        backup.requests.forEach {
            val eventId = IdMap.getNewId(Event::class, it.eventId)
                ?: throw IllegalStateException("Event not found")
            val songId = IdMap.getNewId(Song::class, it.songId)
                ?: throw IllegalStateException("Performance not found")
            val requestId = RequestService()
                .create(it.copy(eventId = eventId, songId = songId))
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
    val songs: List<Song>,
    val requests: List<Request>,
    val users: List<User>,
)