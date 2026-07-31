package streetlight.server.db.services

import kampfire.api.EmailAddress
import klutch.db.DbService
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.server.db.tables.BouncedEmailTable
import kotlin.time.Clock

class BouncedEmailTableDao: DbService() {
    suspend fun createBouncedEmail(email: EmailAddress, reason: String) = dbQuery {
        BouncedEmailTable.insert {
            it[BouncedEmailTable.email] = email.value
            it[BouncedEmailTable.reason] = reason
            it[BouncedEmailTable.bouncedAt] = Clock.System.now()
        }
    }

    suspend fun readIsBounced(email: EmailAddress) = dbQuery {
        BouncedEmailTable.selectAll().where { BouncedEmailTable.email.eq(email.value) }.any() // is any the right function?
    }
}