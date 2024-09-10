package streetlight.server.db.services

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import streetlight.model.dto.RequestInfo
import streetlight.server.db.ApiService
import streetlight.server.db.tables.EventEntity
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.db.tables.RequestEntity
import streetlight.server.db.tables.RequestTable
import streetlight.server.db.tables.SongEntity
import streetlight.server.db.tables.SongTable

class RequestInfoService : ApiService() {
    suspend fun read(id: Int): RequestInfo? {
        return dbQuery {
            requestInfos
                .where { RequestTable.id eq id }
                .firstOrNull()
                ?.toRequestInfo()
        }
    }

    suspend fun readAll(): List<RequestInfo> {
        return dbQuery {
            requestInfos
                .map { it.toRequestInfo() }
        }
    }

    suspend fun readAllByEvent(eventId: Int): List<RequestInfo> {
        return dbQuery {
            requestInfos
                .where { RequestTable.eventId eq eventId }
                .map { it.toRequestInfo() }
        }
    }

    suspend fun getQueue(eventId: Int): List<RequestInfo> {
        return dbQuery {
            requestInfos
                .where { RequestTable.eventId eq eventId and (RequestTable.performed eq false) }
                .orderBy(RequestTable.time)
                .map { it.toRequestInfo() }
        }
    }

    suspend fun getRandomRequest(eventId: Int): RequestInfo? {
        return dbQuery {
            val event = EventEntity.findById(eventId) ?: return@dbQuery null

            val songCount = SongEntity.count().toInt()
            if (songCount == 0) return@dbQuery null

            // val lastTenMinutes = System.currentTimeMillis() - 10 * 60 * 1000

            val songCountColumn = SongTable.id.count().alias("SongCount")

            val song =
                SongTable.join(RequestTable, JoinType.LEFT, SongTable.id, RequestTable.songId)
                    .select(
                        songCountColumn,
                        SongTable.id,
                        SongTable.name,
                        SongTable.artist,
                        SongTable.userId,
                        RequestTable.eventId,
                    )
                    .where {
                        RequestTable.eventId.isNull() or (RequestTable.eventId eq eventId) // and (RequestTable.time.isNull() or (RequestTable.time less lastTenMinutes))
                    }
                    .groupBy(SongTable.id)
                    .orderBy(songCountColumn)
                    .firstOrNull() ?: return@dbQuery null

            val id = RequestTable.insertAndGetId {
                it[RequestTable.eventId] = event.id
                it[songId] = song[SongTable.id]
                it[time] = System.currentTimeMillis()
                it[performed] = false
                it[notes] = "auto"
            }

            if (id.value == 0) return@dbQuery null
            requestInfos
                .where { RequestTable.id eq id.value }
                .firstOrNull()
                ?.toRequestInfo()
        }
    }
}

val requestInfoColumns = listOf(
    RequestTable.id,
    EventTable.id,
    LocationTable.name,
    SongTable.id,
    SongTable.name,
    SongTable.artist,
    RequestTable.notes,
    RequestTable.requesterName,
    RequestTable.time,
    RequestTable.performed,
)

val requestInfos: Query
    get() = RequestTable
        .join(EventTable, JoinType.INNER, EventTable.id, RequestTable.eventId)
        .join(LocationTable, JoinType.INNER, LocationTable.id, EventTable.location)
        .join(SongTable, JoinType.INNER, SongTable.id, RequestTable.songId)
        .select(requestInfoColumns)

fun ResultRow.toRequestInfo(): RequestInfo = RequestInfo(
    id = this[RequestTable.id].value,
    eventId = this[EventTable.id].value,
    locationName = this[LocationTable.name],
    songId = this[SongTable.id].value,
    songName = this[SongTable.name],
    requesterName = this[RequestTable.requesterName],
    notes = this[RequestTable.notes],
    artist = this[SongTable.artist],
    time = this[RequestTable.time],
    performed = this[RequestTable.performed],
)

fun RequestEntity.toRequestInfo(): RequestInfo = RequestInfo(
    this.id.value,
    this.event.id.value,
    this.event.location.name,
    this.song.id.value,
    this.song.name,
    this.song.artist,
    this.notes,
    this.requesterName,
    this.time,
    this.performed
)