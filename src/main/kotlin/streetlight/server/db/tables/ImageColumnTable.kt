package streetlight.server.db.tables

import kampfire.model.ImageSize
import koala.Image
import org.jetbrains.exposed.v1.core.Column
import streetlight.model.data.ImageTarget

/** A table holding an [image] column, whose images are stored at the sizes of [imageTarget]. */
interface ImageColumnTable {
    val image: Column<Image?>
    val imageTarget: ImageTarget
}

/** The sizes an image for this target is stored at. */
val ImageTarget.sizes get() = when (this) {
    ImageTarget.Event, ImageTarget.Star, ImageTarget.Offering ->
        listOf(ImageSize.Medium, ImageSize.Small, ImageSize.Thumb)
    ImageTarget.Location, ImageTarget.Galaxy, ImageTarget.City, ImageTarget.Media, ImageTarget.Block,
    ImageTarget.Talent ->
        listOf(ImageSize.Large, ImageSize.Medium, ImageSize.Small, ImageSize.Thumb)
}
