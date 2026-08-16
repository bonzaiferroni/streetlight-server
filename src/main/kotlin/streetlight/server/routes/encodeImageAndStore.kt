package streetlight.server.routes

import kampfire.model.ImageSize
import kampfire.model.ImageVariant
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.Url
import kampfire.model.largest
import kampfire.model.toDataOr
import klutch.db.model.CallerId
import koala.Image
import koala.ImageId
import streetlight.model.data.ImageFormat
import streetlight.model.data.ImageRecord
import streetlight.server.model.DataScope
import streetlight.server.utils.toStarId
import kotlin.time.Clock

suspend fun DataScope.encodeImageAndStore(
    bytes: ByteArray,
    callerId: CallerId?,
    sizes: List<ImageSize>,
    meta: Image? = null
): Outcome<Image> {
    val imageId = ImageId.random()
    val filenameRoot = imageId.toString()

    val result = encodeImage(bytes, sizes).toDataOr { return it }
    val format = result.format
    val now = Clock.System.now()

    val variants = result.encodings.map {
        val encodedBytes = it.bytes; val size = it.size
        val filename = "$filenameRoot-${size.label}.${format.ext}"
        val url = saveS3ImageFile(encodedBytes, format, filename)
            ?: error("unable to save image: $filename")
        log("saved remote image: $filename")
        ImageVariant(size, url)
    }.takeIf { it.isNotEmpty() } ?: return ImageProblem.Saving

    val url = variants.largest ?: error("largest image not found")

    val image = Image(
        url = url,
        imageId = imageId,
        variants = variants,
        aspect = result.aspect,
        name = meta?.name,
        description = meta?.description,
        attribution = meta?.attribution,
        attributionUrl = meta?.attributionUrl,
        caption = meta?.caption,
    )

    dao.image.create(ImageRecord(
        imageId = imageId,
        starId = callerId?.toStarId(),
        format = format,
        image = image,
        updatedAt = now,
        createdAt = now,
    ))

    return Ok(image)
}

suspend fun DataScope.saveS3ImageFile(
    bytes: ByteArray,
    format: ImageFormat,
    filename: String? = null,
): Url? {
    val fileId = ImageId.random()
    val filename = filename ?: fileId.value.toString()

    return client.blob.put(bytes, filename, format.contentType)
}

object ImageProblem {
    val Encoding = Problem("Unable to encode image.")
    val Download = Problem("Unable to download image.")
    val Saving = Problem("Unable to save image.")
    val ZeroDimension = Problem("Image has zero height or width.")
    val ZeroFrames = Problem("Animated image has zero frames.")
    val InvalidFormat = Problem("Invalid image format.")
}