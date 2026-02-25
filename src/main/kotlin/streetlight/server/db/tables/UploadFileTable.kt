package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import kampfire.model.UserId
import klutch.db.tables.UserTable
import klutch.utils.toStringId
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.*
import streetlight.server.utils.toProjectId

object UploadFileTable : UUIDTable("user_file") {
    val userId = reference("user_id", UserTable, ReferenceOption.CASCADE).nullable()
    val url = text("url")
    val fileType = enumeration<FileType>("file_type")
    val fileUse = enumeration<FileUse>("file_use")
    val fileFormat = enumeration<FileFormat>("file_format")
    val createdAt = datetime("created_at")
}

fun ResultRow.toUploadFile() = UploadFile(
    uploadFileId = toProjectId(UploadFileTable.id),
    userId = this[UploadFileTable.userId]?.value?.let { UserId(it.toStringId()) },
    url = this[UploadFileTable.url],
    fileType = this[UploadFileTable.fileType],
    fileUse = this[UploadFileTable.fileUse],
    fileFormat = this[UploadFileTable.fileFormat],
    createdAt = this[UploadFileTable.createdAt].toInstantFromUtc()
)

fun UpdateBuilder<*>.writeFull(userFile: UploadFile) {
    this[UploadFileTable.id] = userFile.uploadFileId.toUUID()
    this[UploadFileTable.userId] = userFile.userId?.toUUID()
    this[UploadFileTable.createdAt] = userFile.createdAt.toLocalDateTimeUtc()
    writeUpdate(userFile)
}

fun UpdateBuilder<*>.writeUpdate(userFile: UploadFile) {
    this[UploadFileTable.url] = userFile.url
    this[UploadFileTable.fileType] = userFile.fileType
    this[UploadFileTable.fileUse] = userFile.fileUse
    this[UploadFileTable.fileFormat] = userFile.fileFormat
}
