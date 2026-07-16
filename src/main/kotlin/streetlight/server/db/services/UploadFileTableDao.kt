package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import streetlight.model.data.StarId
import streetlight.model.data.UploadFile
import streetlight.model.data.UploadFileId
import streetlight.model.data.toRecordId
import streetlight.server.db.tables.UploadFileTable
import streetlight.server.db.tables.toUploadFile
import streetlight.server.db.tables.createRecord

class UploadFileTableDao : DbService() {

    suspend fun readUserFile(uploadFileId: UploadFileId) = dbQuery {
        UploadFileTable.read { it.id.eq(uploadFileId) }.firstOrNull()?.toUploadFile()
    }

    suspend fun readUserFiles(userId: StarId) = dbQuery {
        UploadFileTable.read { UploadFileTable.starId.eq(userId.value) }.map { it.toUploadFile() }
    }

    suspend fun readUserFiles(userId: StarId, count: Int) = dbQuery {
        UploadFileTable.read { UploadFileTable.starId.eq(userId.value) }
            .orderBy(UploadFileTable.createdAt, SortOrder.DESC_NULLS_LAST)
            .limit(count)
            .map { it.toUploadFile() }
    }

    suspend fun create(userFile: UploadFile): UploadFileId = dbQuery {
        UploadFileTable.insertAndGetId {
            it.createRecord(userFile)
        }.value.toRecordId()
    }
}
