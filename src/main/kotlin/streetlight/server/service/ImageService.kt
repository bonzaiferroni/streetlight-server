package streetlight.server.service

import kampfire.api.TableId
import kampfire.model.HttpProblem
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
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import streetlight.model.data.ImageFormat
import streetlight.model.data.ImageRecord
import streetlight.server.db.tables.ImageColumnTable
import streetlight.server.db.tables.sizes
import streetlight.server.model.DataScope
import streetlight.server.utils.toStarId
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * The image to save on the row [rowId] of [table]: `null` for a blank image, the current one when unchanged, or a
 * new one stored at the sizes of the table's target.
 */
suspend fun <T> DataScope.checkImageAndStore(
    callerId: CallerId?,
    rowId: TableId<Uuid>?,
    image: Image?,
    table: T,
): Outcome<Image?> where T: UuidTable, T: ImageColumnTable {
    // associate with user when imageRef is relative
    val userId = callerId.takeIf { image?.isRelative ?: false }

    if (image == null || image.value.isBlank()) {
        // removes any existing image
        return Ok(null)
    }
    val currentImage = rowId?.let {
        dao.image.readTableImage(table, it)
    }
    if (currentImage == image) return Ok(currentImage)
    return provisionImageAndStore(userId, image, table.imageTarget.sizes)
}


/** The stored image of [image]: read by its id, or downloaded from its URL and stored at [sizes]. */
suspend fun DataScope.provisionImageAndStore(
    userId: CallerId?,
    image: Image,
    sizes: List<ImageSize>,
): Outcome<Image> {
    if (sizes.isEmpty()) error("image sizes must be defined")
    return when (val imageId = image.imageId) {
        null -> {
            if (!image.isAbsolute) error("image url is not absolute")
            val bytes = client.image.download(image.url) ?: return ImageProblem.Download
            encodeImageAndStore(bytes, userId, sizes)
        }
        else -> {
            Ok(dao.image.readImage(imageId) ?: return HttpProblem.NotFound)
        }
    }
}

/**
 * Encodes [bytes] at each of [sizes], stores each file, and records the image, taking its name and attribution
 * from [meta].
 */
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
        ImageVariant(size, url)
    }.takeIf { it.isNotEmpty() } ?: return ImageProblem.Saving
    log("saved ${variants.size} remote images: $filenameRoot")

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
    val AnimatedWebp = Problem("Animated WebP is not supported. Use a GIF instead.")
}
