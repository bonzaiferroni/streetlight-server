package streetlight.server.db.tables

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object RequestTable : IntIdTable() {
    val eventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE)
    val songId = reference("song_id", SongTable, onDelete = ReferenceOption.CASCADE)
    val time = long("time")
    val performed = bool("performed")
    val notes = text("notes")
    val requesterName = text("requester_name").nullable()
}

class RequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, RequestEntity>(RequestTable)

    var event by EventEntity referencedOn RequestTable.eventId
    var song by SongEntity referencedOn RequestTable.songId
    var time by RequestTable.time
    var performed by RequestTable.performed
    var notes by RequestTable.notes
    var requesterName by RequestTable.requesterName
}