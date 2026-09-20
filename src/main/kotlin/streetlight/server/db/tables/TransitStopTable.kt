package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import streetlight.model.data.TransitStop
import streetlight.model.data.TransitStopId

object TransitStopTable : IdTable<String>("transit_stop") {
    override val id = text("id").entityId()
    val name = text("name")
    val latitude = double("latitude")
    val longitude = double("longitude")
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toTransitStop() = TransitStop(
    transitStopId = TransitStopId(this[TransitStopTable.id].value),
    name = this[TransitStopTable.name],
    latitude = this[TransitStopTable.latitude],
    longitude = this[TransitStopTable.longitude],
    description = this[TransitStopTable.description],
)

fun UpdateBuilder<*>.createTransitStop(transitStop: TransitStop) {
    this[TransitStopTable.id] = transitStop.transitStopId.value
    updateTransitStop(transitStop)
}

fun UpdateBuilder<*>.updateTransitStop(transitStop: TransitStop) {
    this[TransitStopTable.name] = transitStop.name
    this[TransitStopTable.latitude] = transitStop.latitude
    this[TransitStopTable.longitude] = transitStop.longitude
    this[TransitStopTable.description] = transitStop.description
}
