package streetlight.server.db.services

import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import klutch.utils.toStringId
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import streetlight.model.data.NewTalent
import streetlight.model.data.Talent
import streetlight.model.data.TalentId
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.TalentTable
import streetlight.server.db.tables.toTalent
import streetlight.server.db.tables.writeFull
import streetlight.server.db.tables.writeUpdate

class TalentTableDao : DbService() {

    suspend fun readTalent(talentId: TalentId) = dbQuery {
        TalentTable.read { it.id.eq(talentId) }.firstOrNull()?.toTalent()
    }

    suspend fun readTalents() = dbQuery {
        TalentTable.selectAll().map { it.toTalent() }
    }

    suspend fun readUserTalents(userId: UserId) = dbQuery {
        TalentTable.read { it.userId.eq(userId) }.map { it.toTalent() }
    }

    suspend fun create(talent: Talent, userId: UserId): TalentId = dbQuery {
        TalentTable.insertAndGetId {
            it.writeFull(talent, userId)
        }.value.toStringId().toProjectId()
    }

    suspend fun create(talent: NewTalent, userId: UserId): Talent? = dbQuery {
        val id = TalentId.random()
        TalentTable.insert {
            it.writeFull(Talent(
                talentId = id,
                name = talent.name,
                description = talent.description,
                imageUrl = talent.imageUrl,
                experience = 0,
                talentType = talent.talentType
            ), userId)
        }
        readTalent(id)
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
