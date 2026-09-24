package streetlight.server.routes

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.format.Format
import com.sksamuel.scrimage.format.FormatDetector
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.StreamingGifWriter
import com.sksamuel.scrimage.webp.Gif2WebpWriter
import com.sksamuel.scrimage.webp.WebpWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import kampfire.model.ImageSize
import kampfire.model.Ok
import kampfire.model.Outcome
import klutch.utils.logger
import streetlight.model.data.ImageFormat
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger(::encodeImage)

private const val WEBP_QUALITY = 75
private const val WEBP_EFFORT = 6

@Suppress("ArrayInDataClass")
data class ImageEncoding(
    val size: ImageSize,
    val bytes: ByteArray,
)

data class EncodingResult(
    val format: ImageFormat,
    val encodings: List<ImageEncoding>,
    val aspect: Float,
)

/** Resizes an image to each of [sizes], keeping a GIF animated. Fails for an unknown format or an animated WebP. */
fun encodeImage(
    bytes: ByteArray,
    sizes: List<ImageSize>,
): Outcome<EncodingResult> {
    println("Received ${bytes.size} bytes, first 8: ${bytes.take(8).map { it.toUByte() }}")
    val format = FormatDetector.detect(bytes.inputStream()).orElse(null) ?: return ImageProblem.InvalidFormat

    if (format == Format.WEBP && bytes.isAnimatedWebp()) return ImageProblem.AnimatedWebp

    return if (format == Format.GIF) {
        resizeAnimatedImage(format, bytes, sizes)
    } else {
        resizeStaticImage(format, bytes, sizes)
    }
}

private fun resizeStaticImage(
    format: Format,
    bytes: ByteArray,
    sizes: List<ImageSize>,
): Outcome<EncodingResult> {
    val image = ImmutableImage.loader().fromBytes(bytes)
    if (image.height == 0) return ImageProblem.ZeroDimension
    val writer = WebpWriter.DEFAULT.withQ(WEBP_QUALITY).withM(WEBP_EFFORT)
    val aspectRatio = image.width / image.height.toFloat()

    val encodings = sizes.mapNotNull { size ->
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
        }.onFailure { logger.error { it } }
            .getOrNull()
    }.takeIf { it.isNotEmpty() } ?: return ImageProblem.Encoding

    return Ok(EncodingResult(ImageFormat.WEBP, encodings, aspectRatio))
}

/**
 * Resizes an animated GIF and converts to animated WebP.
 *
 * The path is: resize each frame → write intermediate GIF via
 * StreamingGifWriter → re-read as AnimatedGif → convert to WebP
 * via Gif2WebpWriter.
 */
private fun resizeAnimatedImage(
    format: Format,
    bytes: ByteArray,
    sizes: List<ImageSize>,
): Outcome<EncodingResult> {
    val gif = AnimatedGifReader.read(ImageSource.of(bytes))
    val frameCount = gif.frameCount
    if (frameCount <= 0) return ImageProblem.ZeroFrames

    val firstFrame = gif.getFrame(0)
    if (firstFrame.height == 0) return ImageProblem.ZeroDimension
    val aspectRatio = firstFrame.width / firstFrame.height.toFloat()
    val animatedWriter = Gif2WebpWriter.DEFAULT.withLossy().withQ(WEBP_QUALITY).withM(WEBP_EFFORT)
    val delay = runCatching { gif.getDelay(0) }
        .getOrDefault(Duration.ofMillis(200))

    val encodings = sizes.mapNotNull { size ->
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
            val webpBytes = resizedGif.bytes(animatedWriter)

            ImageEncoding(size, webpBytes)
        }.onFailure { logger.error { it } }
            .getOrNull()
    }.takeIf { it.isNotEmpty() } ?: return ImageProblem.Encoding

    return Ok(EncodingResult(ImageFormat.WEBP, encodings, aspectRatio))
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
    require(aspectRatio == null || aspectRatio > 0f) { "aspectRatio must be > 0" }

    if (aspectRatio == null) {
        if (sourceWidth <= size) return sourceWidth to sourceHeight
        val targetHeight = ((size.toDouble() * sourceHeight) / sourceWidth)
            .roundToInt()
            .coerceIn(1, sourceHeight)
        return size to targetHeight
    }

    val requestedHeight = (size / aspectRatio).roundToInt().coerceAtLeast(1)
    val scale = minOf(
        1f,
        sourceWidth.toFloat() / size,
        sourceHeight.toFloat() / requestedHeight,
    )

    return (size * scale).roundToInt().coerceIn(1, sourceWidth) to
            (requestedHeight * scale).roundToInt().coerceIn(1, sourceHeight)
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

// VP8X chunk starts at byte 12, its flags byte is at 20, animation is bit 1
private fun ByteArray.isAnimatedWebp(): Boolean =
    size > 20 && decodeToString(12, 16) == "VP8X" && (this[20].toInt() and 0x02) != 0

private fun Format.toImageFormat() = when(this) {
    Format.PNG -> ImageFormat.PNG
    Format.GIF -> ImageFormat.GIF
    Format.JPEG -> ImageFormat.JPEG
    Format.WEBP -> ImageFormat.WEBP
}