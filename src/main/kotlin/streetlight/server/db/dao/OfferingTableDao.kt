package streetlight.server.db.dao

import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.orIfNotNull
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.OfferingEdit
import streetlight.model.data.OfferingId
import streetlight.model.data.StarId
import streetlight.server.db.tables.OfferingTable
import streetlight.server.db.tables.createOffering
import streetlight.server.db.tables.toOffering
import streetlight.server.db.tables.updateOffering
import kotlin.time.Clock

class OfferingTableDao: DbService() {
    suspend fun readStarOfferings(starId: StarId) = dbQuery {
        OfferingTable.selectAll().where { OfferingTable.starId.eq(starId) }
            .orderBy(OfferingTable.createdAt, SortOrder.ASC)
            .map { it.toOffering() }
    }

    /** The offering [offeringId], or `null` when it is not public and not [callerId]'s. */
    suspend fun readOffering(offeringId: OfferingId, callerId: CallerId?) = dbQuery {
        val isVisible = OfferingTable.isPublic.eq(true) orIfNotNull callerId?.let { OfferingTable.starId.eq(it) }
        OfferingTable.selectAll().where { OfferingTable.id.eq(offeringId) and isVisible }
            .singleOrNull()?.toOffering()
    }

    /** The offering [offeringId] of [starId], or `null` when [starId] has no such offering. */
    suspend fun readStarOffering(starId: StarId, offeringId: OfferingId) = dbQuery {
        OfferingTable.selectAll().where { OfferingTable.id.eq(offeringId) and OfferingTable.starId.eq(starId) }
            .singleOrNull()?.toOffering()
    }

    suspend fun create(starId: StarId, edit: OfferingEdit, currency: String) = dbQuery {
        OfferingTable.insertReturning { it.createOffering(starId, edit, currency, Clock.System.now()) }
            .single().toOffering()
    }

    /** Writes [edit] to the offering [offeringId] of [starId], or returns `null` when [starId] has no such offering. */
    suspend fun update(starId: StarId, offeringId: OfferingId, edit: OfferingEdit) = dbQuery {
        OfferingTable.updateReturning(where = {
            OfferingTable.id.eq(offeringId) and OfferingTable.starId.eq(starId)
        }) { it.updateOffering(edit, Clock.System.now()) }
            .singleOrNull()?.toOffering()
    }
}
