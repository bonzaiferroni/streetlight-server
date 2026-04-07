package streetlight.server.routes

import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kabinet.console.globalConsole
import kampfire.model.UserId
import kotlinx.datetime.Clock
import streetlight.model.data.FileFormat
import streetlight.model.data.FileType
import streetlight.model.data.FileUse
import streetlight.model.data.UploadFile
import streetlight.model.data.UploadFileId
import streetlight.server.model.*
import java.io.File

private val console = globalConsole.getHandle("uploader")

suspend fun StreetlightRouting.saveImageFile(
    bytes: ByteArray,
    userId: UserId?,
    fileUse: FileUse,
    format: FileFormat,
    filename: String? = null,
): String? {
    val fileId = UploadFileId.random()
    val filename = filename ?: fileId.value

    val name = "$filename.${format.ext}"

    val url = app.storage.s3.upload(bytes, name, format.contentType) ?: return null
    
    app.dao.userFile.create(
        UploadFile(
            uploadFileId = fileId,
            userId = userId,
            url = url,
            fileType = FileType.Image,
            fileUse = fileUse,
            fileFormat = format,
            createdAt = Clock.System.now()
        )
    )

    return url
}

suspend fun StreetlightRouting.saveFullImage(
    bytes: ByteArray,
    userId: UserId?,
    filename: String? = null,
): String? {
    var forceEncoding = false
    val format = detectFormatFromImage(bytes).let {
        // save BMP as PNG
        if (it == FileFormat.BMP) {
            forceEncoding = true
            FileFormat.PNG
        } else it
    } ?: return null

    val resizedBytes = resizeImage(bytes, format, 1024, null, forceEncoding) ?: return null
    return saveImageFile(resizedBytes, userId, FileUse.FullImage, format, filename)
}

fun detectFormatFromImage(bytes: ByteArray): FileFormat? {
    fun has(prefix: ByteArray): Boolean =
        bytes.size >= prefix.size && prefix.indices.all { i -> bytes[i] == prefix[i] }

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (has(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))) return FileFormat.PNG
    // JPEG: FF D8 FF
    if (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) return FileFormat.JPEG
    // GIF: "GIF87a" or "GIF89a"
    if (bytes.size >= 6) {
        val s = bytes.copyOfRange(0, 6).decodeToString()
        if (s == "GIF87a" || s == "GIF89a") return FileFormat.GIF
    }
    // WEBP: "RIFF" .... "WEBP"
    if (bytes.size >= 12) {
        val riff = bytes.copyOfRange(0, 4).decodeToString()
        val webp = bytes.copyOfRange(8, 12).decodeToString()
        if (riff == "RIFF" && webp == "WEBP") return FileFormat.WEBP
    }
    // BMP: "BM"
    if (bytes.size >= 2 && bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()) return FileFormat.BMP

    return null
}

suspend fun StreetlightRouting.downloadAndSaveImage(imageUrl: String?): String? {
    if (imageUrl == null || !imageUrl.startsWith("http")) return imageUrl
    val bytes = downloadImage(imageUrl) ?: return null
    return saveFullImage(bytes, null, null)
}

suspend fun RoutingContext.validateImage(
    bytes: ByteArray,
): FileFormat? {
    if (bytes.isEmpty()) {
        call.respond(HttpStatusCode.BadRequest, "Empty body")
        return null
    }

    // Size guard (example: 32 MB)
    val maxBytes = 32 * 1024 * 1024
    if (bytes.size > maxBytes) {
        call.respond(HttpStatusCode.PayloadTooLarge, "Too large")
        return null
    }

    val fileFormat = detectFormatFromImage(bytes)
    if (fileFormat == null) {
        call.respond(HttpStatusCode.UnsupportedMediaType, "Not a supported image")
        return null
    }

    // Optional: compare with declared Content-Type (don’t trust it, just sanity check)
    val declared = call.request.contentType()
    if (declared.contentType.isNotBlank() && declared.contentType != "image") {
        call.respond(HttpStatusCode.UnsupportedMediaType, "Declared Content-Type not image")
        return null
    }

    return fileFormat
}