package streetlight.server.db.tables

import io.ktor.http.Url
import kampfire.model.ImageSize
import klutch.utils.toUUID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.ProjectId
import streetlight.server.model.StreetlightRouting
import streetlight.server.routes.SaveImageResult
import streetlight.server.routes.resizeOriginalImage

class TableImageConfig(
    val table: UUIDTable,
    val refColumn: Column<Url?>,
    val sizeColumns: List<ImageColumnConfig>
) {
}

data class ImageColumnConfig(
    val column: Column<Url?>,
    val size: ImageSize
)

data class ImageValues(
    val imageUrl: Url,
    val sizeValues: List<SaveImageResult>
)

fun UpdateBuilder<*>.writeImages(
    config: TableImageConfig,
    values: ImageValues?,
) {
    val values = values ?: return
    this[config.refColumn] = values.imageUrl
    val imageSizes = values.sizeValues
    config.sizeColumns.forEach { columnConfig ->
        val column = columnConfig.column; val size = columnConfig.size
        this[column] = imageSizes.firstOrNull { it.size == size}?.url
    }
}

suspend fun deleteCurrentImages(rowId: ProjectId?, imageUrl: String?, config: TableImageConfig) {
    val uuid = rowId?.toUUID() ?: return
    val currentImageUrl = config.table.select(config.refColumn).where { config.table.id.eq(uuid) }
        .firstOrNull()?.getOrNull(config.refColumn) ?: return
    if (imageUrl == currentImageUrl) return
    // do the deletion, locally and s3
}

suspend fun StreetlightRouting.saveImages(imageUrl: String?, config: TableImageConfig): ImageValues? {
    val imageUrl = imageUrl ?: return null
    val results = resizeOriginalImage(imageUrl, config.sizeColumns.map { it.size }) ?: return null
    return ImageValues(imageUrl, results)
}