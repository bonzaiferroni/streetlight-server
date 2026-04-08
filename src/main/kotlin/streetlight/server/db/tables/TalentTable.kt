package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import kampfire.model.UserId
import klutch.db.tables.UserTable
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock
import streetlight.model.data.Talent
import streetlight.model.data.TalentId
import streetlight.model.data.TalentLevel
import streetlight.model.data.TalentType
import streetlight.server.utils.toProjectId

object TalentTable : UUIDTable("talent") {
    val userId = reference("user_id", UserTable, ReferenceOption.CASCADE)
    val name = text("name")
    val description = text("description").nullable()
    val imageUrl = text("image_url").nullable()
    val experience = integer("experience")
    val talentType = enumeration<TalentType>("talent_type")
    val talentLevel = enumeration<TalentLevel>("talent_level")
    val yearStarted = integer("year_started")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toTalent() = Talent(
    talentId = toProjectId<TalentId>(TalentTable.id),
    name = this[TalentTable.name],
    description = this[TalentTable.description],
    imageUrl = this[TalentTable.imageUrl],
    experience = this[TalentTable.experience],
    talentType = this[TalentTable.talentType],
    talentLevel = this[TalentTable.talentLevel],
    yearStarted = this[TalentTable.yearStarted],
    updatedAt = this[TalentTable.updatedAt],
    createdAt = this[TalentTable.createdAt],
)

// Updaters
fun UpdateBuilder<*>.writeFull(talent: Talent, userId: UserId) {
    this[TalentTable.id] = talent.talentId.value.toUUID()
    this[TalentTable.userId] = userId.value.toUUID()
    this[TalentTable.createdAt] = talent.createdAt
    writeUpdate(talent)
}

fun UpdateBuilder<*>.writeUpdate(talent: Talent) {
    this[TalentTable.name] = talent.name
    this[TalentTable.description] = talent.description
    this[TalentTable.imageUrl] = talent.imageUrl
    this[TalentTable.experience] = talent.experience
    this[TalentTable.talentType] = talent.talentType
    this[TalentTable.talentLevel] = talent.talentLevel
    this[TalentTable.yearStarted] = talent.yearStarted
    this[TalentTable.updatedAt] = Clock.System.now()
}
