package streetlight.server.db.dao

import kampfire.api.Username
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import streetlight.model.data.SkillEdit
import streetlight.model.data.Skill
import streetlight.model.data.SkillId
import streetlight.model.data.toRecordId
import streetlight.server.db.tables.SkillTable
import streetlight.server.db.tables.StarTable
import streetlight.server.db.tables.toSkill
import streetlight.server.db.tables.createSkill
import streetlight.server.db.tables.updateSkill
import streetlight.server.utils.toRecordId

class SkillTableDao : DbService() {

    suspend fun readSkill(skillId: SkillId) = dbQuery {
        SkillTable.read { it.id.eq(skillId) }.firstOrNull()?.toSkill()
    }

    suspend fun readSkills() = dbQuery {
        SkillTable.selectAll().map { it.toSkill() }
    }

    suspend fun readUserSkills(callerId: CallerId) = dbQuery {
        SkillTable.read { it.starId.eq(callerId) }.map { it.toSkill() }
    }

    /** The skills of the star [username]. */
    suspend fun readStarSkills(username: Username) = dbQuery {
        SkillTable.innerJoin(StarTable).select(SkillTable.columns)
            .where { StarTable.username.eq(username) }
            .map { it.toSkill() }
    }

    suspend fun create(skill: Skill, callerId: CallerId): SkillId = dbQuery {
        SkillTable.insertAndGetId {
            it.createSkill(skill, callerId)
        }.value.toRecordId()
    }

    suspend fun create(skill: SkillEdit, callerId: CallerId): Skill? = dbQuery {
        val id: SkillId = SkillTable.insertAndGetId {
            it.createSkill(Skill(
                skillId = SkillId.random(),
                username = null,
                name = skill.name,
                description = skill.description,
                image = skill.image,
                experience = 0,
                tags = skill.tags,
                interview = skill.interview,
                links = skill.links,
                skillLevel = skill.skillLevel,
                yearStarted = skill.yearStarted,
                updatedAt = Clock.System.now(),
                createdAt = Clock.System.now(),
            ), callerId)
        }.toRecordId()
        SkillTable.read { it.id.eq(id) }.firstOrNull()?.toSkill()
    }

    suspend fun edit(skillId: SkillId, skill: SkillEdit, callerId: CallerId) = dbQuery {
        val updatedRows = SkillTable.update(where = { SkillTable.id.eq(skillId) and SkillTable.starId.eq(callerId) }) {
            it.updateSkill(Skill(
                skillId = skillId,
                username = null,
                name = skill.name,
                description = skill.description,
                image = skill.image,
                experience = 0,
                tags = skill.tags,
                interview = skill.interview,
                links = skill.links,
                skillLevel = skill.skillLevel,
                yearStarted = skill.yearStarted,
                updatedAt = Clock.System.now(),
                createdAt = Clock.System.now(),
            ))
        }
        if (updatedRows == 1) {
            SkillTable.read { it.id.eq(skillId) }.firstOrNull()?.toSkill()
        } else null
    }

    suspend fun update(skill: Skill) = dbQuery {
        SkillTable.update(where = { SkillTable.id.eq(skill.skillId) }) {
            it.updateSkill(skill)
        } == 1
    }

    suspend fun delete(skillId: SkillId) = dbQuery {
        SkillTable.deleteWhere { SkillTable.id.eq(skillId) } == 1
    }
}
