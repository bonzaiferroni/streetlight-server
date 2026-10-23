package streetlight.server.service

import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.toDataOr
import klutch.db.model.CallerId
import streetlight.model.data.Skill
import streetlight.model.data.SkillEdit
import streetlight.server.db.tables.SkillTable
import streetlight.server.model.DataScope

/** Creates a skill of [callerId] from [edit], or updates the one it names, failing when they have no such skill. */
suspend fun DataScope.editSkill(callerId: CallerId, edit: SkillEdit): Outcome<Skill> = transaction {
    val skillId = edit.skillId
    val image = checkImageAndStore(callerId, skillId, edit.image, SkillTable)
        .toDataOr { return@transaction it }
    val stored = edit.copy(image = image)

    val skill = when (skillId) {
        null -> dao.skill.create(stored, callerId)
        else -> dao.skill.edit(skillId, stored, callerId)
    } ?: return@transaction HttpProblem.NotFound
    Ok(skill)
}
