package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import klutch.db.tables.UserTable
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.Guest
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toUserIdOrNull

object GuestTable : UUIDTable("guest") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val name = text("name").nullable()
    val songs = jsonb<List<String>>("songs", tableJsonDefault).nullable()
    val createdAt = datetime("created_at")
}

fun ResultRow.toGuest() = Guest(
    guestId = toProjectId(GuestTable.id),
    userId = toUserIdOrNull(GuestTable.userId),
    name = this[GuestTable.name],
    songs = this[GuestTable.songs],
    createdAt = this[GuestTable.createdAt].toInstantFromUtc(),
)

fun UpdateBuilder<*>.writeFull(guest: Guest) {
    this[GuestTable.id] = guest.guestId.toUUID()
    this[GuestTable.userId] = guest.userId?.toUUID()
    this[GuestTable.createdAt] = guest.createdAt.toLocalDateTimeUtc()
    writeUpdate(guest)
}

fun UpdateBuilder<*>.writeUpdate(guest: Guest) {
    this[GuestTable.name] = guest.name
    this[GuestTable.songs] = guest.songs
}
