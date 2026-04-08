package streetlight.server.db.services

import kampfire.model.UserId
import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import klutch.utils.toStringId
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import streetlight.model.data.UploadFile
import streetlight.model.data.UploadFileId
import streetlight.model.data.toProjectId
import streetlight.server.db.tables.UploadFileTable
import streetlight.server.db.tables.toUploadFile
import streetlight.server.db.tables.writeFull

class UploadFileTableDao : DbService() {

    suspend fun readUserFile(uploadFileId: UploadFileId) = dbQuery {
        UploadFileTable.read { it.id.eq(uploadFileId) }.firstOrNull()?.toUploadFile()
    }

    suspend fun readUserFiles(userId: UserId) = dbQuery {
        UploadFileTable.read { UploadFileTable.userId.eq(userId) }.map { it.toUploadFile() }
    }

    suspend fun readUserFiles(userId: UserId, count: Int) = dbQuery {
        UploadFileTable.read { UploadFileTable.userId.eq(userId) }
            .orderBy(UploadFileTable.createdAt, SortOrder.DESC_NULLS_LAST)
            .limit(count)
            .map { it.toUploadFile() }
    }

    suspend fun create(userFile: UploadFile): UploadFileId = dbQuery {
        UploadFileTable.insertAndGetId {
            it.writeFull(userFile)
        }.value.toStringId().toProjectId()
    }
}
