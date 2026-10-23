package streetlight.server.db.tables

import kampfire.api.toUsername
import klutch.db.SyncValueTrigger
import klutch.db.image
import klutch.db.jsonbConfig
import klutch.db.model.CallerId
import klutch.utils.transformMarkdown
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock
import org.jetbrains.exposed.v1.json.jsonb
import koala.model.ExtraLink
import streetlight.model.data.ImageTarget
import streetlight.model.data.InterviewQuestion
import streetlight.model.data.Skill
import streetlight.model.data.SkillId
import streetlight.model.data.SkillLevel
import streetlight.model.data.SkillTag
import streetlight.server.utils.toRecordId

object SkillTable : UuidTable("skill"), ImageColumnTable {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    val username = text("username").nullable()
    val name = text("name")
    val description = text("description").transformMarkdown().nullable()
    override val image = image("image").nullable()
    override val imageTarget = ImageTarget.Skill
    val experience = integer("experience")
    val tags = array<String>("tags").nullable()
    val interview = jsonb<List<InterviewQuestion>>("interview", jsonbConfig).nullable()
    val links = jsonb<List<ExtraLink>>("links", jsonbConfig).nullable()
    val skillLevel = enumeration<SkillLevel>("skill_level")
    val yearStarted = integer("year_started")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    init {
        index(null, false, tags, indexType = "GIN")
    }
}

val skillUsernameSync = SyncValueTrigger(SkillTable.starId, SkillTable.username, StarTable, StarTable.username)

fun ResultRow.toSkill() = Skill(
    skillId = toRecordId<SkillId>(SkillTable.id),
    username = this[SkillTable.username]?.toUsername(),
    name = this[SkillTable.name],
    description = this[SkillTable.description],
    image = this[SkillTable.image],
    experience = this[SkillTable.experience],
    tags = this[SkillTable.tags]?.map { SkillTag.valueOf(it) },
    interview = this[SkillTable.interview].orEmpty(),
    links = this[SkillTable.links],
    skillLevel = this[SkillTable.skillLevel],
    yearStarted = this[SkillTable.yearStarted],
    updatedAt = this[SkillTable.updatedAt],
    createdAt = this[SkillTable.createdAt],
)

// Updaters
fun UpdateBuilder<*>.createSkill(skill: Skill, callerId: CallerId) {
    this[SkillTable.id] = skill.skillId.value
    this[SkillTable.starId] = callerId.value
    this[SkillTable.createdAt] = skill.createdAt
    updateSkill(skill)
}

fun UpdateBuilder<*>.updateSkill(skill: Skill) {
    this[SkillTable.name] = skill.name
    this[SkillTable.description] = skill.description
    this[SkillTable.image] = skill.image
    this[SkillTable.experience] = skill.experience
    this[SkillTable.tags] = skill.tags?.takeIf { it.isNotEmpty() }?.map { it.name }
    this[SkillTable.interview] = skill.interview.takeIf { it.isNotEmpty() }
    this[SkillTable.links] = skill.links?.takeIf { it.isNotEmpty() }
    this[SkillTable.skillLevel] = skill.skillLevel
    this[SkillTable.yearStarted] = skill.yearStarted
    this[SkillTable.updatedAt] = Clock.System.now()
}
