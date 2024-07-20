package streetlight.server.db.services

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object RequestTable : IntIdTable() {
    val event = reference("event_id", EventTable)
    val song = reference("song_id", SongTable)
    val time = long("time")
    val performed = bool("performed")
    val notes = text("notes")
}

class RequestEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, RequestEntity>(RequestTable)

    var event by EventEntity referencedOn RequestTable.event
    var song by SongEntity referencedOn RequestTable.song
    var time by RequestTable.time
    var performed by RequestTable.performed
    var notes by RequestTable.notes
}