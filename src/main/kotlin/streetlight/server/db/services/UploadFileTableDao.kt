package streetlight.server.db.services

import kampfire.model.BasicUserId
import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
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

    suspend fun readUserFiles(userId: BasicUserId) = dbQuery {
        UploadFileTable.read { UploadFileTable.starId.eq(userId.value) }.map { it.toUploadFile() }
    }

    suspend fun readUserFiles(userId: BasicUserId, count: Int) = dbQuery {
        UploadFileTable.read { UploadFileTable.starId.eq(userId.value) }
            .orderBy(UploadFileTable.createdAt, SortOrder.DESC_NULLS_LAST)
            .limit(count)
            .map { it.toUploadFile() }
    }

    suspend fun create(userFile: UploadFile): UploadFileId = dbQuery {
        UploadFileTable.insertAndGetId {
            it.writeFull(userFile)
        }.value.toProjectId()
    }
}
