package streetlight.server.db.tables

import kampfire.api.TableId
import klutch.utils.eq
import klutch.utils.inList
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.SkillId
import streetlight.server.utils.toRecordId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** A join table linking records of one kind to skills: [skillId], the record's [recordId], and when they were linked. */
interface SkillLinkTable {
    val skillId: Column<EntityID<Uuid>>
    val recordId: Column<EntityID<Uuid>>
    val createdAt: Column<Instant>
}

/** The skills each medium is linked to. */
object SkillMediaTable: Table("skill_media"), SkillLinkTable {
    override val skillId = reference("skill_id", SkillTable, onDelete = ReferenceOption.CASCADE)
    override val recordId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE).index()
    override val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(skillId, recordId)
}

/** The skills each offering is linked to. */
object SkillOfferingTable: Table("skill_offering"), SkillLinkTable {
    override val skillId = reference("skill_id", SkillTable, onDelete = ReferenceOption.CASCADE)
    override val recordId = reference("offering_id", OfferingTable, onDelete = ReferenceOption.CASCADE).index()
    override val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(skillId, recordId)
}

/** The ids of the skills [recordId] is linked to. */
fun <T> T.readSkillIds(recordId: TableId<Uuid>): List<SkillId> where T: Table, T: SkillLinkTable =
    select(skillId)
        .where { this@readSkillIds.recordId.eq(recordId) }
        .map { it.toRecordId<SkillId>(skillId) }

/** A query of the ids of the records linked to [skillId], for a read to filter by with `inSubQuery`. */
fun <T> T.recordIdsOf(skillId: SkillId) where T: Table, T: SkillLinkTable =
    select(recordId).where { this@recordIdsOf.skillId.eq(skillId) }

/** Links [recordId] to the skills of [skillIds] that belong to [ownerId], and unlinks it from every other skill. */
fun <T> T.linkSkills(recordId: TableId<Uuid>, skillIds: List<SkillId>, ownerId: TableId<Uuid>) where T: Table, T: SkillLinkTable {
    val owned = if (skillIds.isEmpty()) emptyList() else SkillTable.select(SkillTable.id)
        .where { SkillTable.id.inList(skillIds) and SkillTable.starId.eq(ownerId) }
        .map { it.toRecordId<SkillId>(SkillTable.id) }
    deleteWhere {
        val ofRecord = this@linkSkills.recordId.eq(recordId)
        if (owned.isEmpty()) ofRecord else ofRecord and not(skillId.inList(owned))
    }
    val linked = readSkillIds(recordId).toSet()
    val now = Clock.System.now()
    batchInsert(owned.filterNot { it in linked }) { id ->
        this[skillId] = id.value
        this[this@linkSkills.recordId] = recordId.value
        this[createdAt] = now
    }
}
