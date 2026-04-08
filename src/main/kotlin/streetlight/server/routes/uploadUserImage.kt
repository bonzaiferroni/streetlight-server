package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.model.ImageSize
import kampfire.model.Url
import kampfire.model.UserId
import kampfire.model.toUrl
import kotlin.time.Clock
import streetlight.model.data.FileFormat
import streetlight.model.data.FileType
import streetlight.model.data.StorageType
import streetlight.model.data.UploadFile
import streetlight.model.data.UploadFileId
import streetlight.server.model.*
import java.io.File

private val console = globalConsole.getHandle("uploader")

suspend fun StreetlightRouting.saveLocalImageFile(
    bytes: ByteArray,
    userId: UserId?,
    filename: String? = null,
    format: FileFormat? = null,
): Url? {
    val fileId = UploadFileId.random()
    val format = format ?: detectFormatFromImage(bytes) ?: return null
    val filename = filename ?: fileId.value

    val name = "$filename.${format.ext}"

    val file = File(uploadFolder, name)
    val url = "/${uploadFolder.name}/$name".toUrl()
    file.writeBytes(bytes)

    app.dao.userFile.create(
        UploadFile(
            uploadFileId = fileId,
            userId = userId,
            url = url,
            fileType = FileType.Image,
            size = null,
            fileFormat = format,
            storage = StorageType.Local,
            createdAt = Clock.System.now()
        )
    )

    return url
}

suspend fun StreetlightRouting.saveS3ImageFile(
    bytes: ByteArray,
    userId: UserId?,
    size: ImageSize,
    format: FileFormat,
    filename: String? = null,
): Url? {
    val fileId = UploadFileId.random()
    val filename = filename ?: fileId.value

    val url = app.storage.s3.put(bytes, filename, format.contentType) ?: return null
    
    app.dao.userFile.create(
        UploadFile(
            uploadFileId = fileId,
            userId = userId,
            url = url,
            fileType = FileType.Image,
            size = size,
            fileFormat = format,
            storage = StorageType.S3,
            createdAt = Clock.System.now()
        )
    )

    return url
}
