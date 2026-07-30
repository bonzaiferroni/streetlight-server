package streetlight.server.db.tables

import klutch.db.model.CallerId
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import streetlight.model.data.StarId
import kotlin.uuid.Uuid

interface StarIdTable {
    val starId: Column<EntityID<Uuid>>
}

fun <T> T.getConstraint(callerId: CallerId?) where T: Table, T: StarIdTable = getConstraint(callerId) { starId.eq(it) }

fun getConstraint(callerId: CallerId?, constraint: (CallerId) -> Op<Boolean>): () -> Op<Boolean> =
    callerId?.let { { constraint(it) } } ?: { Op.FALSE }