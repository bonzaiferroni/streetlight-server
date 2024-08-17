package streetlight.server.db.services

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import streetlight.model.dto.RequestInfo
import streetlight.server.db.ApiService
import javax.management.Query.or
import kotlin.math.min
import kotlin.text.Typography.greater

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
            val playedSongs = RequestTable.select(RequestTable.id, RequestTable.songId)
                .where { RequestTable.performed eq true }
                .orderBy(RequestTable.id, SortOrder.DESC)
                .limit(min(songCount - 1, 10))
                .map { it[RequestTable.songId] }
                .toList()

            val song =
                SongTable.join(RequestTable, JoinType.LEFT, SongTable.id, RequestTable.songId)
                    .select(
                        songCountColumn,
                        SongTable.id,
                        SongTable.name,
                        SongTable.artist,
                        SongTable.userId
                    )
                    .where {
                        SongTable.id notInList playedSongs // and (RequestTable.time.isNull() or (RequestTable.time less lastTenMinutes))
                    }
                    .groupBy(SongTable.id)
                    .orderBy(songCountColumn)
                    .firstOrNull() ?: return@dbQuery null

            val id = RequestTable.insertAndGetId {
                it[RequestTable.eventId] = event.id
                it[songId] = song[SongTable.id]
                it[time] = System.currentTimeMillis()
                it[performed] = false
                it[notes] = ""
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
    RequestTable.time,
    RequestTable.performed
)

val requestInfos: Query
    get() = RequestTable.innerJoin(EventTable).innerJoin(LocationTable)
        .join(SongTable, JoinType.INNER, SongTable.id, RequestTable.songId)
        .select(requestInfoColumns)

fun ResultRow.toRequestInfo(): RequestInfo = RequestInfo(
    id = this[RequestTable.id].value,
    eventId = this[EventTable.id].value,
    locationName = this[LocationTable.name],
    songId = this[SongTable.id].value,
    songName = this[SongTable.name],
    artist = this[SongTable.artist],
    notes = this[RequestTable.notes],
    time = this[RequestTable.time],
    performed = this[RequestTable.performed],
)