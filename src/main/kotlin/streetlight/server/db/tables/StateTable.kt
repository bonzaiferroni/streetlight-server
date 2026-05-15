package streetlight.server.db.tables

import klutch.utils.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.CountryId
import streetlight.model.data.State
import streetlight.model.data.StateId
import streetlight.server.utils.toProjectId
import kotlin.time.Clock

object StateTable : IntIdTable("state") {
    val name = text("name")
    val countryId = reference("country_id", CountryTable.id)
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(name, countryId)
    }
}

fun ResultRow.toState() = State(
    stateId = StateId(this[StateTable.id].value),
    countryId = CountryId(this[StateTable.countryId].value),
    name = this[StateTable.name],
)

fun UpdateBuilder<*>.writeFull(name: String, countryId: CountryId) {

    writeUpdate(name)
}

fun UpdateBuilder<*>.writeUpdate(name: String) {

}
