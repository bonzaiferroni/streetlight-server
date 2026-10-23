package streetlight.server.service

import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.toDataOr
import klutch.db.model.CallerId
import streetlight.model.data.Talent
import streetlight.model.data.TalentEdit
import streetlight.server.db.tables.TalentTable
import streetlight.server.model.DataScope

/** Creates a talent of [callerId] from [edit], or updates the one it names, failing when they have no such talent. */
suspend fun DataScope.editTalent(callerId: CallerId, edit: TalentEdit): Outcome<Talent> = transaction {
    val talentId = edit.talentId
    val image = checkImageAndStore(callerId, talentId, edit.image, TalentTable)
        .toDataOr { return@transaction it }
    val stored = edit.copy(image = image)

    val talent = when (talentId) {
        null -> dao.talent.create(stored, callerId)
        else -> dao.talent.edit(talentId, stored, callerId)
    } ?: return@transaction HttpProblem.NotFound
    Ok(talent)
}
