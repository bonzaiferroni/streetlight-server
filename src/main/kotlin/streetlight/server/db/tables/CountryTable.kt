package streetlight.server.db.tables

import klutch.utils.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Country
import streetlight.server.utils.toProjectId
import kotlin.time.Clock

object CountryTable : UUIDTable("country") {
    val name = text("name")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toCountry() = Country(
    countryId = toProjectId(CountryTable.id),
    name = this[CountryTable.name],
)

fun UpdateBuilder<*>.writeFull(country: Country) {
    this[CountryTable.id] = country.countryId.toUUID()
    this[CountryTable.createdAt] = Clock.System.now()
    writeUpdate(country)
}

fun UpdateBuilder<*>.writeUpdate(country: Country) {
    this[CountryTable.name] = country.name
    this[CountryTable.updatedAt] = Clock.System.now()
}
