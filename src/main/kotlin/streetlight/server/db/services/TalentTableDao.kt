package streetlight.server.db.services

import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import klutch.utils.toStringId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import streetlight.model.data.TalentEdit
import streetlight.model.data.Talent
import streetlight.model.data.TalentId
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.TalentTable
import streetlight.server.db.tables.toTalent
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate
import streetlight.server.utils.toProjectId

class TalentTableDao : DbService() {

    suspend fun readTalent(talentId: TalentId) = dbQuery {
        TalentTable.read { it.id.eq(talentId) }.firstOrNull()?.toTalent()
    }

    suspend fun readTalents() = dbQuery {
        TalentTable.selectAll().map { it.toTalent() }
    }

    suspend fun readUserTalents(userId: UserId) = dbQuery {
        TalentTable.read { it.starId.eq(userId) }.map { it.toTalent() }
    }

    suspend fun create(talent: Talent, userId: UserId): TalentId = dbQuery {
        TalentTable.insertAndGetId {
            it.writeFull(talent, userId)
        }.value.toStringId().toProjectId()
    }

    suspend fun create(talent: TalentEdit, userId: UserId): Talent? = dbQuery {
        val id: TalentId = TalentTable.insertAndGetId {
            it.writeFull(Talent(
                talentId = TalentId.random(),
                name = talent.name,
                description = talent.description,
                imageUrl = talent.imageUrl,
                experience = 0,
                talentType = talent.talentType,
                talentLevel = talent.talentLevel,
                yearStarted = talent.yearStarted,
                updatedAt = Clock.System.now(),
                createdAt = Clock.System.now(),
            ), userId)
        }.toProjectId()
        TalentTable.read { it.id.eq(id) }.firstOrNull()?.toTalent()
    }

    suspend fun edit(talentId: TalentId, talent: TalentEdit, userId: UserId) = dbQuery {
        val updatedRows = TalentTable.update(where = { TalentTable.id.eq(talentId) and TalentTable.starId.eq(userId) }) {
            it.writeUpdate(Talent(
                talentId = talentId,
                name = talent.name,
                description = talent.description,
                imageUrl = talent.imageUrl,
                experience = 0,
                talentType = talent.talentType,
                talentLevel = talent.talentLevel,
                yearStarted = talent.yearStarted,
                updatedAt = Clock.System.now(),
                createdAt = Clock.System.now(),
            ))
        }
        if (updatedRows == 1) {
            TalentTable.read { it.id.eq(talentId) }.firstOrNull()?.toTalent()
        } else null
    }

    suspend fun update(talent: Talent) = dbQuery {
        TalentTable.update(where = { TalentTable.id.eq(talent.talentId) }) {
            it.writeUpdate(talent)
        } == 1
    }

    suspend fun delete(talentId: TalentId) = dbQuery {
        TalentTable.deleteWhere { TalentTable.id.eq(talentId) } == 1
    }
}
