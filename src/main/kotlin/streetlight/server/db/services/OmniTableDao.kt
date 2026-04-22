package streetlight.server.db.services

import klutch.db.DbService
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.OmniRecord
import streetlight.server.db.tables.OmniTable
import streetlight.server.db.tables.write

class OmniTableDao: DbService() {

    suspend fun create(record: OmniRecord) = dbQuery {
        OmniTable.insert {
            it.write(record)
        }
    }

    suspend fun readHistory(count: Int) = dbQuery {
        OmniTable.select(OmniTable.record).orderBy(OmniTable.recordAt, SortOrder.DESC).limit(count)
            .map { it[OmniTable.record] }.sortedBy { it.recordAt }
    }
}