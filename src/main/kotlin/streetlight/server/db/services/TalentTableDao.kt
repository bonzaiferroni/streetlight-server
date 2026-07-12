package streetlight.server.db.services

import kampfire.model.CallerId
import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import streetlight.model.data.TalentEdit
import streetlight.model.data.Talent
import streetlight.model.data.TalentId
import streetlight.model.data.toRecordId
import streetlight.server.db.tables.TalentTable
import streetlight.server.db.tables.toTalent
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.updateRecord
import streetlight.server.utils.toRecordId

class TalentTableDao : DbService() {

    suspend fun readTalent(talentId: TalentId) = dbQuery {
        TalentTable.read { it.id.eq(talentId) }.firstOrNull()?.toTalent()
    }

    suspend fun readTalents() = dbQuery {
        TalentTable.selectAll().map { it.toTalent() }
    }

    suspend fun readUserTalents(callerId: CallerId) = dbQuery {
        TalentTable.read { it.starId.eq(callerId) }.map { it.toTalent() }
    }

    suspend fun create(talent: Talent, callerId: CallerId): TalentId = dbQuery {
        TalentTable.insertAndGetId {
            it.createRecord(talent, callerId)
        }.value.toRecordId()
    }

    suspend fun create(talent: TalentEdit, callerId: CallerId): Talent? = dbQuery {
        val id: TalentId = TalentTable.insertAndGetId {
            it.createRecord(Talent(
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
            ), callerId)
        }.toRecordId()
        TalentTable.read { it.id.eq(id) }.firstOrNull()?.toTalent()
    }

    suspend fun edit(talentId: TalentId, talent: TalentEdit, callerId: CallerId) = dbQuery {
        val updatedRows = TalentTable.update(where = { TalentTable.id.eq(talentId) and TalentTable.starId.eq(callerId) }) {
            it.updateRecord(Talent(
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
            it.updateRecord(talent)
        } == 1
    }

    suspend fun delete(talentId: TalentId) = dbQuery {
        TalentTable.deleteWhere { TalentTable.id.eq(talentId) } == 1
    }
}
