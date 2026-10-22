package streetlight.server.db.tables

import klutch.db.model.CallerId
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock
import streetlight.model.data.Talent
import streetlight.model.data.TalentId
import streetlight.model.data.TalentLevel
import streetlight.model.data.TalentTag
import streetlight.server.utils.toRecordId

object TalentTable : UuidTable("talent") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    val name = text("name")
    val description = text("description").nullable()
    val imageUrl = text("image_url").nullable()
    val experience = integer("experience")
    val tags = array<String>("tags").nullable()
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
    imageUrl = this[TalentTable.imageUrl],
    experience = this[TalentTable.experience],
    tags = this[TalentTable.tags]?.map { TalentTag.valueOf(it) },
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
    this[TalentTable.imageUrl] = talent.imageUrl
    this[TalentTable.experience] = talent.experience
    this[TalentTable.tags] = talent.tags?.takeIf { it.isNotEmpty() }?.map { it.name }
    this[TalentTable.talentLevel] = talent.talentLevel
    this[TalentTable.yearStarted] = talent.yearStarted
    this[TalentTable.updatedAt] = Clock.System.now()
}
