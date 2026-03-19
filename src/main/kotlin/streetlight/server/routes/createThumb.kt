package streetlight.server.routes

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.StreamingGifWriter
import kabinet.console.globalConsole
import streetlight.model.data.FileFormat
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.time.Duration

private val console = globalConsole.getHandle("thumbs")

fun createThumb(path: String, size: Int = 128): String? {
    val serverPath = "../$path"
    val file = File(serverPath)
    if (!file.exists() || !file.isFile) return null

    val bytes = runCatching { file.readBytes() }
        .onFailure { console.logThrowable(it) }
        .getOrNull() ?: return null

    val format = formatFromPath(path) ?: return null

    val outFile = when (format) {
        FileFormat.GIF -> File(file.parentFile, "${file.nameWithoutExtension}_thumb_$size.gif")
        else -> File(file.parentFile, "${file.nameWithoutExtension}_thumb_$size.jpg")
    }

    val ok = createThumb(bytes, format, outFile, size)

    if (!ok) return null
    return outFile.absolutePath.split("..")[1]
}

fun createThumb(bytes: ByteArray, filename: String, size: Int = 128): String? {
    val format = detectFileTypeFromImage(bytes) ?: return null
    val path = "/upload/$filename.${format.ext}"
    val file = File("..$path")
    val ok = createThumb(bytes, format, file, size)
    return if (ok) path else null
}

fun createThumb(bytes: ByteArray, format: FileFormat, outFile: File, size: Int = 128): Boolean {
    return when (format) {
        FileFormat.GIF -> createAnimatedGifThumb(bytes, outFile, size)
        else -> createStaticThumbAsJpg(bytes, outFile, size)
    }
}

private fun formatFromPath(path: String): FileFormat? {
    val ext = path.substringAfterLast('.', "").lowercase()
    return FileFormat.entries.firstOrNull { it.ext == ext } ?: when (ext) {
        "jpeg" -> FileFormat.JPEG
        else -> null
    }
}

private fun createStaticThumbAsJpg(bytes: ByteArray, outFile: File, size: Int): Boolean {
    return runCatching {
        outFile.parentFile?.mkdirs()

        val image = ImmutableImage.loader().fromBytes(bytes)
        val scaled = image.cover(size, size)

        val rgbAwt = ensureRgbOnWhite(scaled.awt())
        val rgb = ImmutableImage.fromAwt(rgbAwt, BufferedImage.TYPE_INT_RGB)

        val writer = JpegWriter()
            .withCompression(92)
            .withProgressive(true)

        rgb.output(writer, outFile)
        true
    }.onFailure { console.logThrowable(it) }
        .getOrDefault(false)
}

private fun createAnimatedGifThumb(bytes: ByteArray, outFile: File, size: Int): Boolean {
    return runCatching {
        outFile.parentFile?.mkdirs()

        val gif = AnimatedGifReader.read(ImageSource.of(bytes))
        val frameCount = gif.getFrameCount()
        if (frameCount <= 0) return@runCatching false

        val delay = runCatching { gif.getDelay(0) }
            .getOrDefault(Duration.ofMillis(200))

        val writer = StreamingGifWriter(delay, true, true)
        val stream = writer.prepareStream(outFile.absolutePath, BufferedImage.TYPE_INT_ARGB)

        try {
            for (i in 0 until frameCount) {
                val frame = gif.getFrame(i).cover(size, size)
                stream.writeFrame(frame)
            }
        } finally {
            stream.close()
        }

        true
    }.onFailure { console.logThrowable(it) }
        .getOrDefault(false)
}

private fun ensureRgbOnWhite(src: BufferedImage): BufferedImage {
    // Proper flatten: draw onto white first
    val fixed = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)
    val gf = fixed.createGraphics()
    gf.useQuality()
    gf.background = java.awt.Color.WHITE
    gf.clearRect(0, 0, fixed.width, fixed.height)
    gf.drawImage(src, 0, 0, null)
    gf.dispose()

    return fixed
}

private fun Graphics2D.useQuality() {
    setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
}