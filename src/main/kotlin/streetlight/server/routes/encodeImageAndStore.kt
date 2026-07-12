package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.model.ImageSize
import kampfire.model.ImageVariant
import kampfire.model.Url
import streetlight.model.data.FileFormat
import streetlight.model.data.StarId
import streetlight.server.model.DataScope

private val console = globalConsole.getHandle("storeImage")

suspend fun DataScope.storeLocalImage(
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

suspend fun DataScope.encodeImageAndStore(
    bytes: ByteArray,
    userId: StarId?,
    filename: String,
    sizes: List<ImageSize>,
): ImageSizerResult? {
    val filenameRoot = filename.takeIf { !it.contains('.') } ?: filename.split('.').dropLast(1).joinToString(".")

    val result = encodeImage(bytes, sizes) ?: return null
    val urlList = result.encodings.map {
        val encodedBytes = it.bytes; val format = it.format; val size = it.size
        val filename = "$filenameRoot-${size.label}.${format.ext}"
        val url = saveS3ImageFile(encodedBytes, userId, size, format, filename)
            ?: error("unable to save image: $filename")
        console.log("saved remote image: $filename")
        ImageVariant(size, url)
    }.takeIf { it.isNotEmpty() } ?: return null

    return ImageSizerResult(urlList, result.aspectRatio)
}


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

private data class FormatAndEncodingMode(
    val format: FileFormat,
    val forceEncoding: Boolean
)