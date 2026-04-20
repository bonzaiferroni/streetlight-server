package streetlight.server.routes

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpWriter
import kabinet.console.globalConsole
import kampfire.model.ImageSize
import streetlight.model.data.FileFormat
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration
import kotlin.math.roundToInt

private val console = globalConsole.getHandle("resize")

@Suppress("ArrayInDataClass")
data class ImageEncoding(
    val size: ImageSize,
    val bytes: ByteArray,
    val format: FileFormat = FileFormat.WEBP,
)

fun encodeImage(
    bytes: ByteArray,
    sizes: List<ImageSize>,
): List<ImageEncoding> {
    val format = FormatDetector.detect(bytes.inputStream()).orElse(null)

    return if (format == Format.GIF) {
        resizeAnimatedImage(bytes, sizes)
    } else {
        resizeStaticImage(bytes, sizes)
    }
}

private fun resizeStaticImage(
    bytes: ByteArray,
    sizes: List<ImageSize>,
): List<ImageEncoding> {
    val image = ImmutableImage.loader().fromBytes(bytes)
    val writer = WebpWriter.DEFAULT.withQ(80)

    return sizes.mapNotNull { size ->
        runCatching {
            if (image.width < size.minWidthPx) return@mapNotNull null

            val (w, h) = targetDimensions(
                sourceWidth = image.width,
                sourceHeight = image.height,
                size = size.widthPx,
                aspectRatio = size.aspectRatio,
            )

            val scaled = image.cover(w, h)
            val argb = ensureArgb(scaled.awt())
            val output = ImmutableImage.fromAwt(argb, BufferedImage.TYPE_INT_ARGB)
            ImageEncoding(size, output.bytes(writer))
        }.onFailure { console.logThrowable(it) }
            .getOrNull()
    }
}

/**
 * Resizes an animated GIF and converts to animated WebP.
 *
 * The path is: resize each frame → write intermediate GIF via
 * StreamingGifWriter → re-read as AnimatedGif → convert to WebP
 * via Gif2WebpWriter.
 */
private fun resizeAnimatedImage(
    bytes: ByteArray,
    sizes: List<ImageSize>,
): List<ImageEncoding> {
    val gif = AnimatedGifReader.read(ImageSource.of(bytes))
    val frameCount = gif.frameCount
    if (frameCount <= 0) return emptyList()

    val firstFrame = gif.getFrame(0)
    val delay = runCatching { gif.getDelay(0) }
        .getOrDefault(Duration.ofMillis(200))

    return sizes.mapNotNull { size ->
        runCatching {
            if (firstFrame.width < size.minWidthPx) return@mapNotNull null

            val (w, h) = targetDimensions(
                sourceWidth = firstFrame.width,
                sourceHeight = firstFrame.height,
                size = size.widthPx,
                aspectRatio = size.aspectRatio,
            )

            // Step 1: write resized frames to an intermediate GIF
            val gifWriter = StreamingGifWriter(delay, true, true)
            val gifOut = ByteArrayOutputStream()
            val stream = gifWriter.prepareStream(gifOut, BufferedImage.TYPE_INT_ARGB)

            try {
                for (i in 0 until frameCount) {
                    val frame = gif.getFrame(i).cover(w, h)
                    stream.writeFrame(frame)
                }
            } finally {
                stream.close()
            }

            // Step 2: re-read intermediate GIF and convert to animated WebP
            val resizedGif = AnimatedGifReader.read(ImageSource.of(gifOut.toByteArray()))
            val webpBytes = resizedGif.bytes(Gif2WebpWriter.DEFAULT)

            ImageEncoding(size, webpBytes)
        }.onFailure { console.logThrowable(it) }
            .getOrNull()
    }
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

private fun ensureArgb(src: BufferedImage): BufferedImage {
    if (src.type == BufferedImage.TYPE_INT_ARGB) return src
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