package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Country
import streetlight.model.data.CountryId
import kotlin.time.Clock

object CountryTable : IntIdTable("country") {
    val name = text("name").uniqueIndex()
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toCountry() = Country(
    countryId = CountryId(this[CountryTable.id].value),
    name = this[CountryTable.name],
)

fun UpdateBuilder<*>.createCountry(country: Country) {
    this[CountryTable.createdAt] = Clock.System.now()
    updateCountry(country)
}

fun UpdateBuilder<*>.updateCountry(country: Country) {
    this[CountryTable.name] = country.name
    this[CountryTable.updatedAt] = Clock.System.now()
}