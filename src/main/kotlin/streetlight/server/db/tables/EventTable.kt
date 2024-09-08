package streetlight.server.db.tables

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import streetlight.model.core.EventStatus

object EventTable : IntIdTable() {
    val user = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val location = reference("location_id", LocationTable, onDelete = ReferenceOption.CASCADE)
    val timeStart = long("time_start")
    val hours = float("hours").nullable()
    val url = text("url").nullable()
    val imageUrl = text("image_url").nullable()
    val streamUrl = text("stream_url").nullable()
    val name = text("name").nullable()
    val description = text("description").nullable()
    val status = enumerationByName("status", 20, EventStatus::class)
    val currentRequest = reference("current_song_id", RequestTable).nullable()
    val cashTips = float("cash_tips").nullable()
    val cardTips = float("card_tips").nullable()
}

class EventEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, EventEntity>(EventTable)

    var user by UserEntity referencedOn EventTable.user
    var location by LocationEntity referencedOn EventTable.location
    var timeStart by EventTable.timeStart
    var hours by EventTable.hours
    var url by EventTable.url
    var imageUrl by EventTable.imageUrl
    var streamUrl by EventTable.streamUrl
    var name by EventTable.name
    var description by EventTable.description
    var status by EventTable.status
    var currentRequest by RequestEntity optionalReferencedOn EventTable.currentRequest
    var cashTips by EventTable.cashTips
    var cardTips by EventTable.cardTips
    val requests by RequestEntity referrersOn RequestTable.eventId
}