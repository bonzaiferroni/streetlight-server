package streetlight.server.db.tables

import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.Guest
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toProjectIdOrNull

object GuestTable : UUIDTable("guest") {
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val name = text("name").nullable()
    val songs = jsonb<List<String>>("songs", tableJsonDefault).nullable()
    val createdAt = timestamp("created_at")
}

fun ResultRow.toGuest() = Guest(
    guestId = toProjectId(GuestTable.id),
    starId = toProjectIdOrNull(GuestTable.starId),
    name = this[GuestTable.name],
    songs = this[GuestTable.songs],
    createdAt = this[GuestTable.createdAt],
)

fun UpdateBuilder<*>.writeFull(guest: Guest) {
    this[GuestTable.id] = guest.guestId.toUUID()
    this[GuestTable.starId] = guest.starId?.toUUID()
    this[GuestTable.createdAt] = guest.createdAt
    writeUpdate(guest)
}

fun UpdateBuilder<*>.writeUpdate(guest: Guest) {
    this[GuestTable.name] = guest.name
    this[GuestTable.songs] = guest.songs
}
