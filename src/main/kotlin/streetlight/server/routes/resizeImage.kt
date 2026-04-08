package streetlight.server.routes

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.WebpWriter
import kabinet.console.globalConsole
import kampfire.model.ImageSize
import kampfire.model.LARGE_IMAGE_SIZE
import streetlight.model.data.FileFormat
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration
import kotlin.math.roundToInt

private val console = globalConsole.getHandle("resize")

fun resizeImage(
    bytes: ByteArray,
    format: FileFormat,
    size: ImageSize?,
    aspectRatio: Float?,
    forceEncoding: Boolean = false,
) = when (format) {
    FileFormat.JPEG -> resizeJpg(bytes, size, aspectRatio, forceEncoding)
    FileFormat.PNG -> resizePng(bytes, size, aspectRatio, forceEncoding)
    FileFormat.GIF -> resizeGif(bytes, size, aspectRatio, forceEncoding)
    FileFormat.WEBP -> resizeWebp(bytes, size, aspectRatio, forceEncoding)
    FileFormat.BMP -> error("unsupported resize type: BMP")
}

private fun resizeJpg(
    bytes: ByteArray,
    size: ImageSize?,
    aspectRatio: Float?,
    forceEncoding: Boolean,
) = resizeStaticImage(bytes, size, aspectRatio, forceEncoding) { scaled ->
    val rgbAwt = ensureRgbOnBackground(scaled.awt())
    val rgb = ImmutableImage.fromAwt(rgbAwt, BufferedImage.TYPE_INT_RGB)

    val writer = JpegWriter()
        .withCompression(92)
        .withProgressive(true)

    rgb.bytes(writer)
}

private fun resizePng(
    bytes: ByteArray,
    size: ImageSize?,
    aspectRatio: Float?,
    forceEncoding: Boolean,
) = resizeStaticImage(bytes, size, aspectRatio, forceEncoding) { scaled ->
    val argb = ensureArgb(scaled.awt())
    val png = ImmutableImage.fromAwt(argb, BufferedImage.TYPE_INT_ARGB)

    val writer = PngWriter()
        .withCompression(9)

    png.bytes(writer)
}

private fun resizeWebp(
    bytes: ByteArray,
    size: ImageSize?,
    aspectRatio: Float?,
    forceEncoding: Boolean,
) = resizeStaticImage(bytes, size, aspectRatio, forceEncoding) { scaled ->
    val argb = ensureArgb(scaled.awt())
    val webp = ImmutableImage.fromAwt(argb, BufferedImage.TYPE_INT_ARGB)

    val writer = WebpWriter.DEFAULT

    webp.bytes(writer)
}

private inline fun resizeStaticImage(
    bytes: ByteArray,
    size: ImageSize?,
    aspectRatio: Float?,
    forceEncoding: Boolean,
    encode: (ImmutableImage) -> ByteArray,
): ByteArray? = runCatching {
    val image = ImmutableImage.loader().fromBytes(bytes)
    if (size != null && image.width < size.minWidthPx) return null
    val (targetWidth, targetHeight) = targetDimensions(
        sourceWidth = image.width,
        sourceHeight = image.height,
        size = size?.widthPx ?: LARGE_IMAGE_SIZE,
        aspectRatio = aspectRatio,
    )

    if (!forceEncoding && image.width == targetWidth && image.height == targetHeight) {
        return@runCatching bytes
    }

    val scaled = image.cover(targetWidth, targetHeight)
    encode(scaled)
}.onFailure { console.logThrowable(it) }
    .getOrNull()

private fun resizeGif(
    bytes: ByteArray,
    size: ImageSize?,
    aspectRatio: Float?,
    forceEncoding: Boolean,
): ByteArray? {
    return runCatching {
        val gif = AnimatedGifReader.read(ImageSource.of(bytes))
        val frameCount = gif.getFrameCount()
        if (frameCount <= 0) return@runCatching null

        val firstFrame = gif.getFrame(0)
        if (size != null && firstFrame.width < size.minWidthPx) return@runCatching null
        val (targetWidth, targetHeight) = targetDimensions(
            sourceWidth = firstFrame.width,
            sourceHeight = firstFrame.height,
            size = size?.widthPx ?: LARGE_IMAGE_SIZE,
            aspectRatio = aspectRatio,
        )

        if (!forceEncoding && firstFrame.width == targetWidth && firstFrame.height == targetHeight) {
            return@runCatching bytes
        }

        val delay = runCatching { gif.getDelay(0) }
            .getOrDefault(Duration.ofMillis(200))

        val writer = StreamingGifWriter(delay, true, true)
        val out = ByteArrayOutputStream()
        val stream = writer.prepareStream(out, BufferedImage.TYPE_INT_ARGB)

        try {
            for (i in 0 until frameCount) {
                val frame = gif.getFrame(i).cover(targetWidth, targetHeight)
                stream.writeFrame(frame)
            }
        } finally {
            stream.close()
        }

        out.toByteArray()
    }.onFailure { console.logThrowable(it) }
        .getOrNull()
}

private fun targetDimensions(
    sourceWidth: Int,
    sourceHeight: Int,
    size: Int,
    aspectRatio: Float?,
): Pair<Int, Int> {
    require(sourceWidth > 0) { "sourceWidth must be > 0" }
    require(sourceHeight > 0) { "sourceHeight must be > 0" }
    require(size > 0) { "size must be > 0" }

    if (aspectRatio == null) {
        if (sourceWidth <= size) {
            return sourceWidth to sourceHeight
        }
        val targetWidth = size
        val targetHeight = ((targetWidth.toDouble() * sourceHeight) / sourceWidth)
            .roundToInt()
            .coerceIn(1, sourceHeight)

        return targetWidth to targetHeight
    }

    require(aspectRatio > 0f) { "aspectRatio must be > 0" }

    val requestedWidth = (size * aspectRatio)
        .roundToInt()
        .coerceAtLeast(1)
    val requestedHeight = size

    val sourceAspect = sourceWidth.toFloat() / sourceHeight.toFloat()

    val maxWidthAtAspect: Int
    val maxHeightAtAspect: Int

    if (sourceAspect >= aspectRatio) {
        maxHeightAtAspect = sourceHeight
        maxWidthAtAspect = (sourceHeight * aspectRatio)
            .roundToInt()
            .coerceAtMost(sourceWidth)
            .coerceAtLeast(1)
    } else {
        maxWidthAtAspect = sourceWidth
        maxHeightAtAspect = (sourceWidth / aspectRatio)
            .roundToInt()
            .coerceAtMost(sourceHeight)
            .coerceAtLeast(1)
    }

    val widthScale = maxWidthAtAspect.toFloat() / requestedWidth.toFloat()
    val heightScale = maxHeightAtAspect.toFloat() / requestedHeight.toFloat()
    val scale = minOf(1f, widthScale, heightScale)

    val targetWidth = (requestedWidth * scale)
        .roundToInt()
        .coerceIn(1, maxWidthAtAspect)

    val targetHeight = (requestedHeight * scale)
        .roundToInt()
        .coerceIn(1, maxHeightAtAspect)

    return targetWidth to targetHeight
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