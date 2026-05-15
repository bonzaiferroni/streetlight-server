package streetlight.server.db.tables

import klutch.utils.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Country
import streetlight.model.data.CountryId
import streetlight.server.utils.toProjectId
import kotlin.time.Clock

object CountryTable : IntIdTable("country") {
    val name = text("name")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toCountry() = Country(
    countryId = CountryId(this[CountryTable.id].value),
    name = this[CountryTable.name],
)
