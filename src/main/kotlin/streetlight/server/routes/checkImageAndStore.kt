package streetlight.server.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import kampfire.api.TableId
import kampfire.model.ImageSize
import kampfire.model.ImageVariants
import kampfire.utils.randomUuidString
import klutch.db.model.CallerId
import klutch.utils.logger
import koala.Image
import streetlight.model.data.FileFormat
import streetlight.server.db.tables.TableImageConfig
import streetlight.server.model.DataScope
import java.io.File
import kotlin.uuid.Uuid

suspend fun DataScope.checkImageAndStore(
    callerId: CallerId?,
    rowId: TableId<Uuid>?,
    image: Image?,
    config: TableImageConfig
): Image? {
    // associate with user when imageRef is relative
    val userId = callerId.takeIf { image?.isRelative ?: false }

    if (image == null || image.value.isBlank()) {
        // removes any existing image
        return null
    }
    val currentRef = rowId?.let {
        config.readImageRef(it)
    }
    if (currentRef == image) return currentRef
    val result = provisionImageAndStore(userId, image, config.sizes) ?: return null
    return image.copy(aspectRatio = result.aspectRatio, variants = result.variants)
}


suspend fun DataScope.provisionImageAndStore(
    userId: CallerId?,
    image: Image,
    sizes: List<ImageSize>,
): ImageSizerResult? {
    if (sizes.isEmpty()) error("image sizes must be defined")
    return when (image.isAbsolute) {
        true -> {
            val bytes = downloadImage(image.url) ?: return null
            val filename = randomUuidString()
            encodeImageAndStore(bytes, userId, filename, sizes)
        }
        else -> {
            val filename = image.filename ?: error("filename not found: ${image.filename}")
            val bytes = File("..${image}").takeIf { it.isFile }?.readBytes() ?: error("file not found: $image")
            encodeImageAndStore(bytes, userId, filename, sizes)
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

data class ImageSizerResult(val variants: ImageVariants, val aspectRatio: Float)

private val console = KotlinLogging.logger(DataScope::checkImageAndStore)