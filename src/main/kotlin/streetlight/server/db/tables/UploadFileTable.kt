package streetlight.server.db.tables

import kampfire.model.ImageSize
import kampfire.model.UserId
import klutch.db.tables.BasicUserTable
import klutch.db.url
import klutch.utils.toStringId
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.*
import streetlight.server.utils.toProjectId

object UploadFileTable : UUIDTable("user_file") {
    val userId = reference("user_id", BasicUserTable, ReferenceOption.CASCADE).nullable()
    val url = url("url")
    val fileType = enumeration<FileType>("file_type")
    val size = enumeration<ImageSize>("size").nullable()
    val fileFormat = enumeration<FileFormat>("file_format")
    val storage = enumeration<StorageType>("storage")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toUploadFile() = UploadFile(
    uploadFileId = toProjectId(UploadFileTable.id),
    userId = this[UploadFileTable.userId]?.value?.let { UserId(it.toStringId()) },
    url = this[UploadFileTable.url],
    fileType = this[UploadFileTable.fileType],
    size = this[UploadFileTable.size],
    fileFormat = this[UploadFileTable.fileFormat],
    storage = this[UploadFileTable.storage],
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
    this[UploadFileTable.storage] = file.storage
}
