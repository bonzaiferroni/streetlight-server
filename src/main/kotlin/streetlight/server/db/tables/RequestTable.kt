package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Request
import streetlight.server.utils.toRecordId

object RequestTable : UuidTable() {
    val eventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE)
    val songId = reference("song_id", SongTable, onDelete = ReferenceOption.CASCADE)
    val isJoining = bool("is_joining")
    val comment = text("comment").nullable()
    val requesterName = text("requester_name").nullable()
    val createdAt = timestamp("created_at")
}

fun ResultRow.toRequest() = Request(
    requestId = toRecordId(RequestTable.id),
    eventId = toRecordId(RequestTable.eventId),
    songId = toRecordId(RequestTable.songId),
    isJoining = this[RequestTable.isJoining],
    comment = this[RequestTable.comment],
    requesterName = this[RequestTable.requesterName],
    createdAt = this[RequestTable.createdAt],
)

// Updaters
fun UpdateBuilder<*>.createRequest(request: Request) {
    this[RequestTable.id] = request.requestId.value
    this[RequestTable.eventId] = request.eventId.value
    this[RequestTable.songId] = request.songId.value
    updateRequest(request)
}

fun UpdateBuilder<*>.updateRequest(request: Request) {
    this[RequestTable.isJoining] = request.isJoining
    this[RequestTable.comment] = request.comment
    this[RequestTable.requesterName] = request.requesterName
    this[RequestTable.createdAt] = request.createdAt
}