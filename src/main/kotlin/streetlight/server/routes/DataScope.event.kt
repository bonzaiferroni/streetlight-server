package streetlight.server.routes

import kampfire.model.Response
import kampfire.model.Problem
import kampfire.model.toResponse
import streetlight.model.data.EditType
import streetlight.model.data.Event
import streetlight.model.data.EventEdit
import streetlight.model.data.EventId
import streetlight.model.data.StarId
import streetlight.model.data.toEdit
import streetlight.server.db.services.createEditTask
import streetlight.server.db.tables.EventTable
import streetlight.server.model.DataScope

suspend fun DataScope.createEvent(
    callerId: StarId,
    edit: EventEdit,
): Response<Event>? = transaction {
    if (dao.event.hasConflict(edit)) return@transaction Problem("Event already exists")
    val imageSet = saveImages(callerId, edit.eventId, edit.imageRef, EventTable.imageConfig)

    log("creating event: ${edit.title}")
    val event = dao.event.createEvent(callerId, edit, imageSet) ?: return@transaction null
    val editLogId = dao.editLog.create(EditType.Create, event.toEdit(), event.eventId, callerId)
    val star = dao.star.readStar(callerId) ?: error("star not found")

    if (edit.needsReview || star.scoutLevel == 0) {
        createEditTask(editLogId)
    }

    event.toResponse()
}

suspend fun DataScope.updateEvent(
    eventId: EventId,
    callerId: StarId,
    edit: EventEdit,
): Response<Event>? = transaction {
    val imageSet = saveImages(callerId, edit.eventId, edit.imageRef, EventTable.imageConfig)

    log("updating event: ${edit.title}")
    val event = dao.event.updateEvent(eventId, callerId, edit, imageSet) ?: return@transaction null
    val editLogId = dao.editLog.create(EditType.Update, edit, eventId, callerId)

    event.toResponse()
}