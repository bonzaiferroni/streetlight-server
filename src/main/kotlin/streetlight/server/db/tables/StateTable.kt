package streetlight.server.db.tables

import klutch.db.SyncValueTrigger
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.CountryId
import streetlight.model.data.State
import streetlight.model.data.StateId
import kotlin.time.Clock

object StateTable : IntIdTable("state") {
    val name = text("name")
    val countryId = reference("country_id", CountryTable.id)
    val country = text("country")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(name, countryId)
    }
}

val stateCountryTrigger = SyncValueTrigger(StateTable.countryId, StateTable.country, CountryTable, CountryTable.name)

fun ResultRow.toState() = State(
    stateId = StateId(this[StateTable.id].value),
    countryId = CountryId(this[StateTable.countryId].value),
    name = this[StateTable.name],
    country = this[StateTable.country],
)

fun UpdateBuilder<*>.writeFull(state: State) {
    this[StateTable.countryId] = state.countryId.value
    this[StateTable.country] = state.country
    this[StateTable.createdAt] = Clock.System.now()
    writeUpdate(state)
}

fun UpdateBuilder<*>.writeUpdate(state: State) {
    this[StateTable.name] = state.name
    this[StateTable.updatedAt] = Clock.System.now()
}
