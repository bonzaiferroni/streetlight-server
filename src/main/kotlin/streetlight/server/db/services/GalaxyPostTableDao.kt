package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.db.readById
import klutch.utils.eq
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import streetlight.model.data.EventId
import streetlight.model.data.GalaxyId
import streetlight.model.data.GalaxyPost
import streetlight.model.data.GalaxyPostEdit
import streetlight.model.data.GalaxyPostId
import streetlight.server.db.tables.GalaxyPostTable
import streetlight.server.db.tables.toGalaxyPost
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId

class GalaxyPostTableDao : DbService() {

    suspend fun readPost(galaxyPostId: GalaxyPostId) = dbQuery {
        GalaxyPostTable.read { it.id.eq(galaxyPostId) }.firstOrNull()?.toGalaxyPost()
    }

    suspend fun readPosts(galaxyId: GalaxyId) = dbQuery {
        GalaxyPostTable.read { GalaxyPostTable.galaxyId.eq(galaxyId) }.map { it.toGalaxyPost() }
    }

    suspend fun create(edit: GalaxyPostEdit) = dbQuery {
        val post = edit.toGalaxyPost()
        val id = GalaxyPostTable.insertAndGetId { it.writeFull(post) }.value
        GalaxyPostTable.readById(id).toGalaxyPost()
    }

    suspend fun update(galaxyPost: GalaxyPost) = dbQuery {
        GalaxyPostTable.update(where = { GalaxyPostTable.id.eq(galaxyPost.galaxyPostId) }) {
            it.writeUpdate(galaxyPost)
        } == 1
    }

    suspend fun delete(galaxyPostId: GalaxyPostId) = dbQuery {
        GalaxyPostTable.deleteWhere { GalaxyPostTable.id.eq(galaxyPostId) } == 1
    }

//    suspend fun postEvent(galaxyId: GalaxyId, username: String, eventId: EventId) = dbQuery {
//        val post = GalaxyPost(
//            galaxyPostId = GalaxyPostId.random(),
//            galaxyId = galaxyId,
//            username = username,
//            eventId = eventId,
//            locationId = null,
//            text = null,
//            createdAt = Clock.System.now(),
//            updatedAt = Clock.System.now(),
//        )
//    }
}

fun GalaxyPostEdit.toGalaxyPost() = GalaxyPost(
    galaxyPostId = galaxyPostId ?: GalaxyPostId.random(),
    galaxyId = galaxyId ?: error("galaxyId is required"),
    username = username,
    eventId = eventId,
    locationId = locationId,
    text = text,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)