package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kampfire.model.ImageSize
import kampfire.model.Url
import kampfire.model.UserId
import streetlight.model.data.FileFormat
import streetlight.model.data.StorageType
import streetlight.server.model.StreetlightRouting
import java.io.File

suspend fun StreetlightRouting.saveLocalImage(
    bytes: ByteArray,
    userId: UserId?,
    filename: String? = null,
    size: ImageSize = ImageSize.Large
): String? {
    var forceEncoding = false
    val format = detectFormatFromImage(bytes).let {
        // save BMP as PNG
        if (it == FileFormat.BMP) {
            forceEncoding = true
            FileFormat.PNG
        } else it
    } ?: return null
    val resizedBytes = resizeImage(bytes, format, size, null, forceEncoding) ?: return null
    return saveLocalImageFile(resizedBytes, userId, format, filename)
}

suspend fun StreetlightRouting.saveRemoteImage(
    bytes: ByteArray,
    userId: UserId?,
    filename: String? = null,
    sizes: List<ImageSize>,
): List<SaveImageResult>? {
    var forceEncoding = false
    val format = detectFormatFromImage(bytes).let {
        // save BMP as PNG
        if (it == FileFormat.BMP) {
            forceEncoding = true
            FileFormat.PNG
        } else it
    } ?: return null

    val results = sizes.mapNotNull { size ->
        val resizedBytes = resizeImage(bytes, format, size, null, forceEncoding) ?: return@mapNotNull null
        val url = saveS3ImageFile(resizedBytes, userId, size, format, filename) ?: return@mapNotNull null
        SaveImageResult(size, url)
    }

    return results.takeIf { it.isNotEmpty() }
}

suspend fun StreetlightRouting.resizeOriginalImage(
    imageUrl: Url,
    sizes: List<ImageSize>,
): List<SaveImageResult>? {
    return when (imageUrl.isAbsolute) {
        true -> {
            if (!sizes.isEmpty()) {
                val bytes = downloadImage(imageUrl) ?: return null
                saveRemoteImage(bytes, null, null, sizes)
            } else null
        }
        else -> {
            val bytes = File("../${imageUrl}").takeIf { it.isFile }?.readBytes() ?: return null
            saveRemoteImage(bytes, null, null, sizes)
        }
    }
}

data class SaveImageResult(
    val size: ImageSize?,
    val url: Url,
)

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