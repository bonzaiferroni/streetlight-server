package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.sql.insert
import streetlight.model.data.Guest
import streetlight.model.data.GuestId
import streetlight.server.db.tables.GuestTable
import streetlight.server.db.tables.toGuest
import streetlight.server.db.tables.writeFull

class GuestTableDao: DbService() {

    suspend fun readById(guestId: GuestId): Guest? = dbQuery {
        GuestTable.read { it.id.eq(guestId) }.firstOrNull()?.toGuest()
    }

    suspend fun create(guest: Guest) = dbQuery {
        GuestTable.insert { it.writeFull(guest) }
    }
}
