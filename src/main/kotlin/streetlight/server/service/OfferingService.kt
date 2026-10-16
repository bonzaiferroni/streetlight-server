package streetlight.server.service

import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.toDataOr
import klutch.db.model.CallerId
import streetlight.model.data.Offering
import streetlight.model.data.OfferingEdit
import streetlight.model.data.OfferingId
import streetlight.model.data.StarId
import streetlight.server.db.tables.OfferingTable
import streetlight.server.model.DataScope
import streetlight.server.utils.toStarId

/** The offerings of [starId], oldest first. */
suspend fun DataScope.readStarOfferings(starId: StarId): Outcome<List<Offering>> =
    Ok(dao.offering.readStarOfferings(starId))

/** The offering [offeringId], or a failure when it is inactive and not [callerId]'s. */
suspend fun DataScope.readOffering(offeringId: OfferingId, callerId: CallerId?): Outcome<Offering> =
    dao.offering.readOffering(offeringId, callerId)?.let { Ok(it) } ?: HttpProblem.NotFound

/** Creates an offering of [callerId] from [edit], priced in their seller account's currency. */
suspend fun DataScope.createOffering(callerId: CallerId, edit: OfferingEdit): Outcome<Offering> = transaction {
    if (!edit.validity.isValid) return@transaction HttpProblem.BadRequest
    val starId = callerId.toStarId()
    val image = checkImageAndStore(callerId, null, edit.image, OfferingTable)
        .toDataOr { return@transaction it }
    val currency = dao.seller.readSeller(starId, client.payment.provider)?.defaultCurrency ?: DEFAULT_CURRENCY

    log("creating offering: ${edit.title}")
    Ok(dao.offering.create(starId, edit.copy(image = image), currency))
}

/** Updates the offering of [callerId] that [edit] names, or fails when they have no such offering. */
suspend fun DataScope.updateOffering(callerId: CallerId, edit: OfferingEdit): Outcome<Offering> = transaction {
    val offeringId = edit.offeringId ?: return@transaction HttpProblem.BadRequest
    if (!edit.validity.isValid) return@transaction HttpProblem.BadRequest
    val image = checkImageAndStore(callerId, offeringId, edit.image, OfferingTable)
        .toDataOr { return@transaction it }

    log("updating offering: ${edit.title}")
    val offering = dao.offering.update(callerId.toStarId(), offeringId, edit.copy(image = image))
        ?: return@transaction HttpProblem.NotFound
    Ok(offering)
}

private const val DEFAULT_CURRENCY = "usd"
