package streetlight.server.db.tables

import kabinet.utils.toInstantFromUtc
import kabinet.utils.toLocalDateTimeUtc
import kampfire.model.ImageSize
import kampfire.model.UserId
import klutch.db.tables.UserTable
import klutch.utils.toStringId
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.*
import streetlight.server.utils.toProjectId

object UploadFileTable : UUIDTable("user_file") {
    val userId = reference("user_id", UserTable, ReferenceOption.CASCADE).nullable()
    val url = text("url")
    val fileType = enumeration<FileType>("file_type")
    val size = enumeration<ImageSize>("size").nullable()
    val fileFormat = enumeration<FileFormat>("file_format")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toUploadFile() = UploadFile(
    uploadFileId = toProjectId(UploadFileTable.id),
    userId = this[UploadFileTable.userId]?.value?.let { UserId(it.toStringId()) },
    url = this[UploadFileTable.url],
    fileType = this[UploadFileTable.fileType],
    size = this[UploadFileTable.size],
    fileFormat = this[UploadFileTable.fileFormat],
    createdAt = this[UploadFileTable.createdAt]
)

fun UpdateBuilder<*>.writeFull(file: UploadFile) {
    this[UploadFileTable.id] = file.uploadFileId.toUUID()
    this[UploadFileTable.userId] = file.userId?.toUUID()
    this[UploadFileTable.createdAt] = file.createdAt
    writeUpdate(file)
}

fun UpdateBuilder<*>.writeUpdate(file: UploadFile) {
    this[UploadFileTable.url] = file.url
    this[UploadFileTable.fileType] = file.fileType
    this[UploadFileTable.size] = file.size
    this[UploadFileTable.fileFormat] = file.fileFormat
}
