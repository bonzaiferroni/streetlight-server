package streetlight.server.routes

import java.awt.AlphaComposite
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriteParam
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream
import javax.imageio.stream.ImageInputStream
import javax.imageio.stream.ImageOutputStream

fun createThumb(path: String, size: Int): String? {
    val serverPath = "../$path"
    val src = File(serverPath)
    if (!src.exists() || !src.isFile) return null

    val bytes = runCatching { src.readBytes() }.getOrNull() ?: return null
    val format = formatFromPath(path) ?: return null

    val outFile = when (format) {
        FileFormat.GIF -> File(src.parentFile, "${src.nameWithoutExtension}_thumb_$size.gif")
        else -> File(src.parentFile, "${src.nameWithoutExtension}_thumb_$size.jpg")
    }

    val thumbPath = when (format) {
        FileFormat.GIF -> {
            if (createAnimatedGifThumb(bytes, outFile, size)) outFile.absolutePath else null
        }
        else -> {
            if (createStaticThumbAsJpg(bytes, outFile, size)) outFile.absolutePath else null
        }
    } ?: return null
    return thumbPath.split("../")[1]
}

@kotlinx.serialization.Serializable
enum class FileFormat(val ext: String) {
    JPEG("jpg"), PNG("png"), GIF("gif"), WEBP("webp"), BMP("bmp")
}

private fun formatFromPath(path: String): FileFormat? {
    val ext = path.substringAfterLast('.', "").lowercase()
    return FileFormat.entries.firstOrNull { it.ext == ext } ?: when (ext) {
        "jpeg" -> FileFormat.JPEG
        else -> null
    }
}

private fun createStaticThumbAsJpg(bytes: ByteArray, outFile: File, size: Int): Boolean {
    val src = ImageIO.read(ByteArrayInputStream(bytes)) ?: return false
    val thumb: BufferedImage = cropCenterSquare(src)
        .let { scaleTo(it, size, size) }
        .let { ensureRgbOnWhite(it) }

    outFile.parentFile?.mkdirs()

    val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull()
        ?: return false

    return runCatching {
        FileImageOutputStream(outFile).use { ios ->
            writer.output = ios
            val param = writer.defaultWriteParam.apply {
                if (canWriteCompressed()) {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = 0.92f // 0.0..1.0 (higher = less artifacts, bigger file)
                }
                try {
                    compressionType = compressionTypes?.firstOrNull { it.equals("JPEG", true) } ?: compressionType
                } catch (_: Throwable) {}
            }

            writer.write(null, IIOImage(thumb, null, null), param)
            writer.dispose()
        }
        true
    }.getOrDefault(false)
}

private data class GifFrameInfo(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val disposal: String,
    val delayCs: Int
)

private fun createAnimatedGifThumb(bytes: ByteArray, outFile: File, size: Int): Boolean {
    val input: ImageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: return false
    var reader: javax.imageio.ImageReader? = null
    var output: ImageOutputStream? = null
    var writer: javax.imageio.ImageWriter? = null

    try {
        reader = ImageIO.getImageReadersByFormatName("gif").asSequence().firstOrNull() ?: return false
        reader.input = input

        val frameCount = reader.getNumImages(true)
        if (frameCount <= 0) return false

        val (screenW, screenH) = readGifLogicalScreenSize(reader.streamMetadata).let {
            it ?: run {
                val f0 = reader.read(0)
                f0?.width?.let { w -> w to f0.height } ?: return false
            }
        }

        outFile.parentFile?.mkdirs()
        output = ImageIO.createImageOutputStream(outFile) ?: return false

        writer = ImageIO.getImageWritersByFormatName("gif").asSequence().firstOrNull() ?: return false
        writer.output = output
        writer.prepareWriteSequence(null)

        val writeParam: ImageWriteParam = writer.defaultWriteParam
        val loopCount = readGifLoopCount(reader.streamMetadata)

        val canvas = BufferedImage(screenW, screenH, BufferedImage.TYPE_INT_ARGB)
        val gCanvas = canvas.createGraphics().also { it.composite = AlphaComposite.SrcOver }
        var previousCanvasCopy: BufferedImage? = null
        var previousInfo: GifFrameInfo? = null

        for (i in 0 until frameCount) {
            // Apply disposal for PREVIOUS frame before drawing the next frame
            previousInfo?.let { prev ->
                when (prev.disposal) {
                    "restoreToBackgroundColor" -> {
                        val gx = gCanvas
                        gx.composite = AlphaComposite.Clear
                        gx.fillRect(prev.left, prev.top, prev.width, prev.height)
                        gx.composite = AlphaComposite.SrcOver
                    }
                    "restoreToPrevious" -> {
                        val snap = previousCanvasCopy
                        if (snap != null) {
                            gCanvas.composite = AlphaComposite.Src
                            gCanvas.drawImage(snap, 0, 0, null)
                            gCanvas.composite = AlphaComposite.SrcOver
                        }
                    }
                    // "none" and "doNotDispose" -> keep as-is
                }
            }

            val frameImage = reader.read(i) ?: return false
            val frameMeta = reader.getImageMetadata(i)
            val info = readGifFrameInfo(frameMeta)

            // For "restoreToPrevious", snapshot BEFORE drawing this frame
            previousCanvasCopy = if (info.disposal == "restoreToPrevious") copyImage(canvas) else null

            // Draw this frame at its offset onto canvas
            gCanvas.drawImage(frameImage, info.left, info.top, null)

            // Now we have a full composited frame in `canvas`
            val fullFrame = copyImage(canvas)

            // Crop+scale to thumb
            val thumb = cropCenterSquare(fullFrame).let { scaleTo(it, size, size) }
            val bgr = toBgr(thumb)

            val type = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_3BYTE_BGR)
            val outMeta = writer.getDefaultImageMetadata(type, writeParam)
            val configured = configureGifFrameMetadata(
                frameMeta = outMeta,
                delayCentiseconds = info.delayCs,
                loopCount = loopCount,
                isFirstFrame = (i == 0)
            )

            writer.writeToSequence(javax.imageio.IIOImage(bgr, null, configured), writeParam)

            previousInfo = info
        }

        writer.endWriteSequence()
        return true
    } catch (t: Throwable) {
        t.printStackTrace()
        return false
    } finally {
        try { writer?.dispose() } catch (_: Throwable) {}
        try { output?.close() } catch (_: Throwable) {}
        try { reader?.dispose() } catch (_: Throwable) {}
        try { input.close() } catch (_: Throwable) {}
    }
}

private fun readGifLogicalScreenSize(streamMeta: IIOMetadata?): Pair<Int, Int>? {
    if (streamMeta == null) return null
    val fmt = streamMeta.nativeMetadataFormatName ?: return null
    val root = streamMeta.getAsTree(fmt) as? IIOMetadataNode ?: return null
    val lsd = root.getElementsByTagName("LogicalScreenDescriptor").item(0) as? IIOMetadataNode ?: return null
    val w = lsd.getAttribute("logicalScreenWidth").toIntOrNull() ?: return null
    val h = lsd.getAttribute("logicalScreenHeight").toIntOrNull() ?: return null
    return w to h
}

private fun readGifFrameInfo(meta: IIOMetadata): GifFrameInfo {
    val fmt = meta.nativeMetadataFormatName ?: "javax_imageio_gif_image_1.0"
    val root = meta.getAsTree(fmt) as IIOMetadataNode

    val gce = root.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
    val disposal = gce.getAttribute("disposalMethod").ifBlank { "none" }
    val delayCs = gce.getAttribute("delayTime").toIntOrNull() ?: 10

    val id = root.getElementsByTagName("ImageDescriptor").item(0) as IIOMetadataNode
    val left = id.getAttribute("imageLeftPosition").toIntOrNull() ?: 0
    val top = id.getAttribute("imageTopPosition").toIntOrNull() ?: 0
    val w = id.getAttribute("imageWidth").toIntOrNull() ?: 0
    val h = id.getAttribute("imageHeight").toIntOrNull() ?: 0

    return GifFrameInfo(left, top, w, h, disposal, delayCs)
}

private fun copyImage(src: BufferedImage): BufferedImage {
    val dst = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB)
    val g = dst.createGraphics()
    g.composite = AlphaComposite.Src
    g.drawImage(src, 0, 0, null)
    g.dispose()
    return dst
}

private fun toBgr(src: BufferedImage): BufferedImage {
    val dst = BufferedImage(src.width, src.height, BufferedImage.TYPE_3BYTE_BGR)
    val g = dst.createGraphics()
    g.drawImage(src, 0, 0, null)
    g.dispose()
    return dst
}

private fun readGifLoopCount(streamMeta: IIOMetadata?): Int {
    if (streamMeta == null) return 0
    val root = streamMeta.getAsTree("javax_imageio_gif_stream_1.0") as? IIOMetadataNode ?: return 0
    val aes = root.getElementsByTagName("ApplicationExtensions").item(0) as? IIOMetadataNode ?: return 0

    // Find NETSCAPE 2.0 extension if present
    for (i in 0 until aes.length) {
        val node = aes.item(i) as? IIOMetadataNode ?: continue
        if (node.nodeName != "ApplicationExtension") continue
        val id = node.getAttribute("applicationID")
        val auth = node.getAttribute("authenticationCode")
        if (id != "NETSCAPE" || auth != "2.0") continue

        val data = node.userObject as? ByteArray ?: continue
        if (data.size < 3) continue
        val lo = data[1].toInt() and 0xFF
        val hi = data[2].toInt() and 0xFF
        return (hi shl 8) or lo
    }

    return 0
}

private fun configureGifFrameMetadata(
    frameMeta: IIOMetadata,
    delayCentiseconds: Int,
    loopCount: Int,
    isFirstFrame: Boolean
): IIOMetadata {
    val fmt = frameMeta.nativeMetadataFormatName ?: "javax_imageio_gif_image_1.0"
    val root = frameMeta.getAsTree(fmt) as IIOMetadataNode

    val gce = root.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
    gce.setAttribute("disposalMethod", "none")
    gce.setAttribute("userInputFlag", "FALSE")
    gce.setAttribute("transparentColorFlag", "FALSE")
    gce.setAttribute("delayTime", delayCentiseconds.toString())
    gce.setAttribute("transparentColorIndex", "0")

    if (isFirstFrame) {
        // Add looping extension in-image (avoids stream mergeTree shenanigans)
        val aes = IIOMetadataNode("ApplicationExtensions")
        val app = IIOMetadataNode("ApplicationExtension")
        app.setAttribute("applicationID", "NETSCAPE")
        app.setAttribute("authenticationCode", "2.0")
        app.userObject = byteArrayOf(
            0x01,
            (loopCount and 0xFF).toByte(),
            ((loopCount shr 8) and 0xFF).toByte()
        )
        aes.appendChild(app)

        val existing = root.getElementsByTagName("ApplicationExtensions")
        if (existing.length > 0) {
            root.removeChild(existing.item(0) as IIOMetadataNode)
        }
        root.appendChild(aes)
    }

    frameMeta.setFromTree(fmt, root)
    return frameMeta
}

private fun cropCenterSquare(src: BufferedImage): BufferedImage {
    val w = src.width
    val h = src.height
    val side = minOf(w, h)
    val x = (w - side) / 2
    val y = (h - side) / 2
    return src.getSubimage(x, y, side, side)
}

private fun scaleTo(src: BufferedImage, targetW: Int, targetH: Int): BufferedImage {
    val dst = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB)
    val g = dst.createGraphics()
    g.useQuality()
    g.drawImage(src, 0, 0, targetW, targetH, null)
    g.dispose()
    return dst
}

private fun ensureRgbOnWhite(src: BufferedImage): BufferedImage {
    val dst = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)
    val g = dst.createGraphics()
    g.useQuality()
    g.drawImage(src, 0, 0, null)
    g.dispose()
    return dst
}

private fun Graphics2D.useQuality() {
    setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
}

private inline fun <T : AutoCloseable, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        runCatching { close() }
    }
}