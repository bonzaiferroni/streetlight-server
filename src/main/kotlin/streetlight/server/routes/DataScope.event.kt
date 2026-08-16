package streetlight.server.routes

import kampfire.model.CoreProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.toDataOr
import kampfire.model.toOutcome
import klutch.db.model.CallerId
import streetlight.model.data.EditType
import streetlight.model.data.Event
import streetlight.model.data.EventEdit
import streetlight.model.data.EventId
import streetlight.model.data.LocationEdit
import streetlight.model.data.StarId
import streetlight.model.data.toEdit
import streetlight.server.db.services.createEditTask
import streetlight.server.db.tables.EventTable
import streetlight.server.db.tables.LocationTable
import streetlight.server.model.DataScope
import streetlight.server.utils.toStarId

suspend fun DataScope.createEvent(
    callerId: CallerId?,
    edit: EventEdit,
): Outcome<Event> = transaction {
    if (dao.event.hasConflict(edit)) return@transaction Problem("Event already exists")
    val image = checkImageAndStore(callerId, edit.eventId, edit.image, EventTable.imageConfig)
        .toDataOr { return@transaction it }

    log("creating event: ${edit.title}")
    val event = dao.event.createEvent(callerId, edit.copy(image = image)) ?: return@transaction CoreProblem.Something
    // td: create editLog

    Ok(event)
}

suspend fun DataScope.updateEvent(
    eventId: EventId,
    callerId: CallerId,
    edit: EventEdit,
): Outcome<Event> = transaction {
    val image = checkImageAndStore(callerId, edit.eventId, edit.image, EventTable.imageConfig)
        .toDataOr { return@transaction it }

    log("updating event: ${edit.title}")
    val event = dao.event.updateEvent(eventId, callerId, edit.copy(image = image))
        ?: return@transaction CoreProblem.Something
    val editLogId = dao.editLog.create(EditType.Update, edit, eventId, callerId)

    Ok(event)
}