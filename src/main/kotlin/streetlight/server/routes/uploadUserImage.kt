package streetlight.server.routes

import kampfire.model.CallerId
import kampfire.model.ImageSize
import kampfire.model.Url
import kampfire.model.toUrl
import kotlin.time.Clock
import streetlight.model.data.FileFormat
import streetlight.model.data.FileType
import streetlight.model.data.StarId
import streetlight.model.data.StorageType
import streetlight.model.data.UploadFile
import streetlight.model.data.UploadFileId
import streetlight.model.data.toStarId
import streetlight.server.model.*
import java.io.File

// private val console = globalConsole.getHandle("uploader")

suspend fun DaoScope.saveLocalImageFile(
    bytes: ByteArray,
    callerId: CallerId?,
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

    dao.userFile.create(
        UploadFile(
            uploadFileId = fileId,
            starId = callerId?.toStarId(),
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

suspend fun DataScope.saveS3ImageFile(
    bytes: ByteArray,
    callerId: CallerId?,
    size: ImageSize,
    format: FileFormat,
    filename: String? = null,
): Url? {
    val fileId = UploadFileId.random()
    val filename = filename ?: fileId.value.toString()

    val url = client.blob.put(bytes, filename, format.contentType) ?: return null
    
    dao.userFile.create(
        UploadFile(
            uploadFileId = fileId,
            starId = callerId?.toStarId(),
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
