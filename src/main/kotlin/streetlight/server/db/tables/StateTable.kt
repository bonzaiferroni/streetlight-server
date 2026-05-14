package streetlight.server.db.tables

import klutch.utils.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.State
import streetlight.server.utils.toProjectId
import kotlin.time.Clock

object StateTable : UUIDTable("state") {
    val name = text("name")
    val countryId = reference("country_id", CountryTable.id)
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toState() = State(
    stateId = toProjectId(StateTable.id),
    countryId = toProjectId(StateTable.countryId),
    name = this[StateTable.name],
)

fun UpdateBuilder<*>.writeFull(state: State) {
    this[StateTable.id] = state.stateId.toUUID()
    this[StateTable.createdAt] = Clock.System.now()
    writeUpdate(state)
}

fun UpdateBuilder<*>.writeUpdate(state: State) {
    this[StateTable.name] = state.name
    this[StateTable.countryId] = state.countryId.toUUID()
    this[StateTable.updatedAt] = Clock.System.now()
}
