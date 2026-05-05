package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kabinet.console.globalConsole
import kampfire.model.ImageSize
import kampfire.model.ScaledImage
import kampfire.model.Url
import kampfire.utils.randomUuidString
import streetlight.model.data.FileFormat
import streetlight.model.data.ProjectId
import streetlight.model.data.StarId
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.TableImageConfig
import streetlight.server.model.StreetlightRouting
import java.io.File

private val console = globalConsole.getHandle("saveImage")

suspend fun StreetlightRouting.saveLocalImage(
    bytes: ByteArray,
    starId: StarId?,
    filename: String,
    size: ImageSize = ImageSize.Large
): Url? {
    val result = detectFormatAndEncodingMode(bytes) ?: return null
    val format = result.format; val forceEncoding = result.forceEncoding

    val resizedBytes = resizeImage(bytes, format, size, size.aspectRatio, forceEncoding) ?: return null
    return saveLocalImageFile(resizedBytes, starId, filename, format)
}

suspend fun StreetlightRouting.saveRemoteImage(
    bytes: ByteArray,
    userId: StarId?,
    filename: String,
    sizes: List<ImageSize>,
): List<ScaledImage>? {
    val filenameRoot = filename.takeIf { !it.contains('.') } ?: filename.split('.').dropLast(1).joinToString(".")

    val encodings = encodeImage(bytes, sizes)
    val results = encodings.map {
        val encodedBytes = it.bytes; val format = it.format; val size = it.size
        val filename = "$filenameRoot-${size.label}.${format.ext}"
        val url = saveS3ImageFile(encodedBytes, userId, size, format, filename)
            ?: error("unable to save image: $filename")
        console.log("saved remote image: $filename")
        ScaledImage(size, url)
    }

    return results.takeIf { it.isNotEmpty() }
}

suspend fun StreetlightRouting.saveImages(
    userId: StarId?,
    rowId: ProjectId?,
    imageRef: Url?,
    config: TableImageConfig
): SavedImageSet? {
    if (imageRef == null) {
        // removes any existing image
        return SavedImageSet(null, null)
    }
    val currentRef = rowId?.let {
        config.readImageRef(it)
    }
    if (currentRef == imageRef) return null
    val results = saveImageSizes(userId, imageRef, config.sizes) ?: return null
    return SavedImageSet(imageRef, results)
}

private data class FormatAndEncodingMode(
    val format: FileFormat,
    val forceEncoding: Boolean
)

private fun detectFormatAndEncodingMode(bytes: ByteArray): FormatAndEncodingMode? {
    var forceEncoding = false
    val format = detectFormatFromImage(bytes).let { format ->
        // save BMP as PNG
        if (format == FileFormat.BMP) {
            forceEncoding = true
            FileFormat.JPEG
        } else format
    } ?: return null
    return FormatAndEncodingMode(format, forceEncoding)
}

suspend fun StreetlightRouting.saveImageSizes(
    userId: StarId?,
    imageUrl: Url,
    sizes: List<ImageSize>,
): List<ScaledImage>? {
    if (sizes.isEmpty()) error("image sizes must be defined")
    return when (imageUrl.isAbsolute) {
        true -> {
            val bytes = downloadImage(imageUrl) ?: return null
            val filename = randomUuidString()
            saveRemoteImage(bytes, userId, filename, sizes)
        }
        else -> {
            val filename = imageUrl.filename ?: error("filename not found: ${imageUrl.filename}")
            val bytes = File("..${imageUrl}").takeIf { it.isFile }?.readBytes() ?: error("file not found: $imageUrl")
            saveRemoteImage(bytes, userId, filename, sizes)
        }
    }
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