package streetlight.server.db.tables

import kampfire.api.TableId
import kampfire.model.ImageSize
import klutch.db.DbService
import klutch.utils.eq
import koala.Image
import streetlight.model.data.ImageTarget
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.jdbc.select
import kotlin.uuid.Uuid

/** The image column of a table and the [target] that decides the sizes its images are stored at. */
class TableImageConfig(
    val table: UuidTable,
    val column: Column<Image?>,
    val target: ImageTarget,
): DbService() {
    val sizes get() = target.sizes

    suspend fun readCurrentImage(id: TableId<Uuid>) = dbQuery {
        table.select(column).where { table.id.eq(id) }.firstOrNull()?.getOrNull(column)
    }
}

fun <T: UuidTable> imageConfigOf(
    table: T,
    column: Column<Image?>,
    target: ImageTarget,
) = TableImageConfig(
    table = table,
    column = column,
    target = target,
)

/** The sizes an image for this target is stored at. */
val ImageTarget.sizes get() = when (this) {
    ImageTarget.Event, ImageTarget.Star, ImageTarget.Offering ->
        listOf(ImageSize.Medium, ImageSize.Small, ImageSize.Thumb)
    ImageTarget.Location, ImageTarget.Galaxy, ImageTarget.City, ImageTarget.Media, ImageTarget.Block ->
        listOf(ImageSize.Large, ImageSize.Medium, ImageSize.Small, ImageSize.Thumb)
}