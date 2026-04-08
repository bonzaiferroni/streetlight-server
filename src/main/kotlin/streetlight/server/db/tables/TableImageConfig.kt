package streetlight.server.db.tables

import kampfire.model.ImageSize
import kampfire.model.ScaledImage
import kampfire.model.ScaledImageArray
import kampfire.model.Url
import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.ProjectId
import streetlight.server.model.StreetlightRouting
import streetlight.server.routes.saveImageSizes

class TableImageConfig(
    val table: UUIDTable,
    val refColumn: Column<Url?>,
    val arrayColumn: Column<ScaledImageArray?>,
    val sizes: List<ImageSize>
): DbService() {

    suspend fun readImageRef(id: ProjectId) = dbQuery {
        table.select(refColumn).where { table.id.eq(id) }.firstOrNull()?.getOrNull(refColumn)
    }
}

data class SavedImageSet(
    val imageRef: Url?,
    val array: ScaledImageArray?
)

fun <T: UUIDTable> imageConfigOf(
    table: T,
    refColumn: Column<Url?>,
    arrayColumn: Column<ScaledImageArray?>,
    vararg sizes: ImageSize
) = TableImageConfig(
    table = table,
    refColumn = refColumn,
    arrayColumn = arrayColumn,
    sizes = sizes.toList()
)

fun UpdateBuilder<*>.writeImages(
    config: TableImageConfig,
    set: SavedImageSet?,
) {
    val set = set ?: return
    this[config.refColumn] = set.imageRef
    this[config.arrayColumn] = set.array
}

//suspend fun deleteCurrentImages(rowId: ProjectId?, imageUrl: Url?, config: TableImageConfig) {
//    val uuid = rowId?.toUUID() ?: return
//    val currentImageUrl = config.table.select(config.refColumn).where { config.table.id.eq(uuid) }
//        .firstOrNull()?.getOrNull(config.refColumn) ?: return
//    if (imageUrl == currentImageUrl) return
//    // do the deletion, locally and s3
//}