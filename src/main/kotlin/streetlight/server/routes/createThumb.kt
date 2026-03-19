package streetlight.server.routes

import kabinet.console.globalConsole
import kampfire.model.UserId
import streetlight.model.data.FileFormat
import streetlight.model.data.FileUse
import java.io.File

private val console = globalConsole.getHandle("thumbs")

suspend fun createThumbFromUploadedImage(path: String, userId: UserId?, size: Int = 128): String? {
    val serverPath = "../$path"
    val file = File(serverPath)
    if (!file.exists() || !file.isFile) return null

    val originalBytes = runCatching { file.readBytes() }
        .onFailure { console.logThrowable(it) }
        .getOrNull() ?: return null

    val format = formatFromPath(path) ?: return null

    val filename = "${file.nameWithoutExtension}_thumb_$size"
    val thumbBytes = createThumbBytes(originalBytes, format, size) ?: return null

    return saveImageFile(thumbBytes, userId, FileUse.ThumbImage, format, filename)
}

suspend fun saveBytesAsThumb(bytes: ByteArray, filename: String, userId: UserId, size: Int = 128): String? {
    val format = detectFileTypeFromImage(bytes) ?: return null
    val thumbBytes = createThumbBytes(bytes, format, size) ?: return null
    return saveImageFile(thumbBytes, userId, FileUse.ThumbImage, format, filename)
}

suspend fun createThumbIfNull(imageUrl: String?, thumbUrl: String?, userId: UserId?): String? {
    if (imageUrl == null || thumbUrl != null) return thumbUrl
    return createThumbFromUploadedImage(imageUrl, userId)
}

private fun createThumbBytes(bytes: ByteArray, format: FileFormat, size: Int = 128): ByteArray? {
    return when (format) {
        FileFormat.GIF -> resizeGif(bytes, size)
        else -> resizeJpg(bytes, size)
    }
}

private fun formatFromPath(path: String): FileFormat? {
    val ext = path.substringAfterLast('.', "").lowercase()
    return FileFormat.entries.firstOrNull { it.ext == ext } ?: when (ext) {
        "jpeg" -> FileFormat.JPEG
        else -> null
    }
}