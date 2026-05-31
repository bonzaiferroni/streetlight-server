package streetlight.server.db.tables

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

fun <T> T.getConstraint(starId: StarId?): (() -> Op<Boolean>)? where T: Table, T: StarIdTable = when (starId) {
    null -> null
    else -> {
        { this.starId.eq(starId) }
    }
}