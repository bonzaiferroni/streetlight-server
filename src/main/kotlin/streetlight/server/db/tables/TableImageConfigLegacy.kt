package streetlight.server.db.tables

import kampfire.api.TableId
import kampfire.model.ImageSize
import klutch.db.DbService
import klutch.utils.eq
import koala.Image
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.jdbc.select
import kotlin.uuid.Uuid

/** The image column of a table and the sizes its images are stored at. */
class TableImageConfig(
    val table: UuidTable,
    val column: Column<Image?>,
    val sizes: List<ImageSize>
): DbService() {

    suspend fun readCurrentImage(id: TableId<Uuid>) = dbQuery {
        table.select(column).where { table.id.eq(id) }.firstOrNull()?.getOrNull(column)
    }
}

fun <T: UuidTable> imageConfigOf(
    table: T,
    column: Column<Image?>,
    vararg sizes: ImageSize
) = TableImageConfig(
    table = table,
    column = column,
    sizes = sizes.toList()
)