package streetlight.server.routes

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.WebpWriter
import kabinet.console.globalConsole
import streetlight.model.data.FileFormat
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration

private val console = globalConsole.getHandle("resize")

fun resizeImage(bytes: ByteArray, format: FileFormat, size: Int) = when (format) {
    FileFormat.JPEG -> resizeJpg(bytes, size)
    FileFormat.PNG -> resizePng(bytes, size)
    FileFormat.GIF -> resizeGif(bytes, size)
    FileFormat.WEBP -> resizeWebp(bytes, size)
    FileFormat.BMP -> error("unsupported type: BMP")
}

private fun resizeJpg(bytes: ByteArray, size: Int): ByteArray? {
    return runCatching {
        val image = ImmutableImage.loader().fromBytes(bytes)
        val scaled = image.cover(size, size)

        val rgbAwt = ensureRgbOnBackground(scaled.awt())
        val rgb = ImmutableImage.fromAwt(rgbAwt, BufferedImage.TYPE_INT_RGB)

        val writer = JpegWriter()
            .withCompression(92)
            .withProgressive(true)

        rgb.bytes(writer)
    }.onFailure { console.logThrowable(it) }
        .getOrNull()
}

private fun resizePng(bytes: ByteArray, size: Int): ByteArray? {
    return runCatching {
        val image = ImmutableImage.loader().fromBytes(bytes)
        val scaled = image.cover(size, size)

        val argb = ensureArgb(scaled.awt())
        val png = ImmutableImage.fromAwt(argb, BufferedImage.TYPE_INT_ARGB)

        val writer = PngWriter()
            .withCompression(9)

        png.bytes(writer)
    }.onFailure { console.logThrowable(it) }
        .getOrNull()
}

private fun resizeWebp(bytes: ByteArray, size: Int): ByteArray? {
    return runCatching {
        val image = ImmutableImage.loader().fromBytes(bytes)
        val scaled = image.cover(size, size)

        val argb = ensureArgb(scaled.awt())
        val webp = ImmutableImage.fromAwt(argb, BufferedImage.TYPE_INT_ARGB)

        val writer = WebpWriter.DEFAULT

        webp.bytes(writer)
    }.onFailure { console.logThrowable(it) }
        .getOrNull()
}

private fun resizeGif(bytes: ByteArray, size: Int): ByteArray? {
    return runCatching {
        val gif = AnimatedGifReader.read(ImageSource.of(bytes))
        val frameCount = gif.getFrameCount()
        if (frameCount <= 0) return@runCatching null

        val delay = runCatching { gif.getDelay(0) }
            .getOrDefault(Duration.ofMillis(200))

        val writer = StreamingGifWriter(delay, true, true)
        val out = ByteArrayOutputStream()
        val stream = writer.prepareStream(out, BufferedImage.TYPE_INT_ARGB)

        try {
            for (i in 0 until frameCount) {
                val frame = gif.getFrame(i).cover(size, size)
                stream.writeFrame(frame)
            }
        } finally {
            stream.close()
        }

        out.toByteArray()
    }.onFailure { console.logThrowable(it) }
        .getOrNull()
}

private fun ensureRgbOnBackground(src: BufferedImage, background: Color = Color.BLACK): BufferedImage {
    val fixed = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)
    val gf = fixed.createGraphics()
    gf.useQuality()
    gf.background = background
    gf.clearRect(0, 0, fixed.width, fixed.height)
    gf.drawImage(src, 0, 0, null)
    gf.dispose()
    return fixed
}

private fun ensureArgb(src: BufferedImage): BufferedImage {
    val fixed = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB)
    val gf = fixed.createGraphics()
    gf.useQuality()
    gf.drawImage(src, 0, 0, null)
    gf.dispose()
    return fixed
}

private fun Graphics2D.useQuality() {
    setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
}