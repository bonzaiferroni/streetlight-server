package streetlight.server.db.tables

import klutch.db.tables.BasicUserTable
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.Guest
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toUserIdOrNull

object GuestTable : UUIDTable("guest") {
    val userId = reference("user_id", BasicUserTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val name = text("name").nullable()
    val songs = jsonb<List<String>>("songs", tableJsonDefault).nullable()
    val createdAt = timestamp("created_at")
}

fun ResultRow.toGuest() = Guest(
    guestId = toProjectId(GuestTable.id),
    userId = toUserIdOrNull(GuestTable.userId),
    name = this[GuestTable.name],
    songs = this[GuestTable.songs],
    createdAt = this[GuestTable.createdAt],
)

fun UpdateBuilder<*>.writeFull(guest: Guest) {
    this[GuestTable.id] = guest.guestId.toUUID()
    this[GuestTable.userId] = guest.userId?.toUUID()
    this[GuestTable.createdAt] = guest.createdAt
    writeUpdate(guest)
}

fun UpdateBuilder<*>.writeUpdate(guest: Guest) {
    this[GuestTable.name] = guest.name
    this[GuestTable.songs] = guest.songs
}
