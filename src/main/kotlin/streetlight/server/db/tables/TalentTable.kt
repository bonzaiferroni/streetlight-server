package streetlight.server.db.tables

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
import streetlight.model.data.ImageTarget
import streetlight.model.data.InterviewQuestion
import streetlight.model.data.Talent
import streetlight.model.data.TalentId
import streetlight.model.data.TalentLevel
import streetlight.model.data.TalentTag
import streetlight.server.utils.toRecordId

object TalentTable : UuidTable("talent"), ImageColumnTable {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    val name = text("name")
    val description = text("description").transformMarkdown().nullable()
    override val image = image("image").nullable()
    override val imageTarget = ImageTarget.Talent
    val experience = integer("experience")
    val tags = array<String>("tags").nullable()
    val interview = jsonb<List<InterviewQuestion>>("interview", jsonbConfig).nullable()
    val talentLevel = enumeration<TalentLevel>("talent_level")
    val yearStarted = integer("year_started")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")

    init {
        index(null, false, tags, indexType = "GIN")
    }
}

fun ResultRow.toTalent() = Talent(
    talentId = toRecordId<TalentId>(TalentTable.id),
    name = this[TalentTable.name],
    description = this[TalentTable.description],
    image = this[TalentTable.image],
    experience = this[TalentTable.experience],
    tags = this[TalentTable.tags]?.map { TalentTag.valueOf(it) },
    interview = this[TalentTable.interview].orEmpty(),
    talentLevel = this[TalentTable.talentLevel],
    yearStarted = this[TalentTable.yearStarted],
    updatedAt = this[TalentTable.updatedAt],
    createdAt = this[TalentTable.createdAt],
)

// Updaters
fun UpdateBuilder<*>.createTalent(talent: Talent, callerId: CallerId) {
    this[TalentTable.id] = talent.talentId.value
    this[TalentTable.starId] = callerId.value
    this[TalentTable.createdAt] = talent.createdAt
    updateTalent(talent)
}

fun UpdateBuilder<*>.updateTalent(talent: Talent) {
    this[TalentTable.name] = talent.name
    this[TalentTable.description] = talent.description
    this[TalentTable.image] = talent.image
    this[TalentTable.experience] = talent.experience
    this[TalentTable.tags] = talent.tags?.takeIf { it.isNotEmpty() }?.map { it.name }
    this[TalentTable.interview] = talent.interview.takeIf { it.isNotEmpty() }
    this[TalentTable.talentLevel] = talent.talentLevel
    this[TalentTable.yearStarted] = talent.yearStarted
    this[TalentTable.updatedAt] = Clock.System.now()
}
