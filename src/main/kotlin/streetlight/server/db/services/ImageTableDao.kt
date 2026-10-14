package streetlight.server.db.services

import kampfire.api.TableId
import klutch.db.DbService
import klutch.db.read
import klutch.db.whereWith
import klutch.utils.eq
import koala.ImageId
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.StarId
import streetlight.model.data.ImageRecord
import streetlight.server.db.tables.ImageColumnTable
import streetlight.server.db.tables.ImageTable
import streetlight.server.db.tables.toImageRecord
import streetlight.server.db.tables.createImage
import streetlight.server.db.tables.toImage
import kotlin.uuid.Uuid

class ImageTableDao : DbService() {

    suspend fun create(userFile: ImageRecord): ImageId = dbQuery {
        ImageTable.insertAndGetId {
            it.createImage(userFile)
        }.value.let { ImageId(it) }
    }

    suspend fun readRecord(imageId: ImageId) = dbQuery {
        ImageTable.read { it.id.eq(imageId) }.firstOrNull()?.toImageRecord()
    }

    /** The image on the row [id] of [table], or `null` when it has none. */
    suspend fun <T> readTableImage(table: T, id: TableId<Uuid>) where T: UuidTable, T: ImageColumnTable = dbQuery {
        table.select(table.image).where { table.id.eq(id) }.firstOrNull()?.getOrNull(table.image)
    }

    suspend fun readImage(imageId: ImageId) = dbQuery {
        ImageTable.select(ImageQuery.columns).whereWith(ImageTable) { id.eq(imageId) }.singleOrNull()?.toImage()
    }

    suspend fun readUserFiles(userId: StarId) = dbQuery {
        ImageTable.read { ImageTable.starId.eq(userId.value) }.map { it.toImageRecord() }
    }

    suspend fun readUserFiles(userId: StarId, count: Int) = dbQuery {
        ImageTable.read { ImageTable.starId.eq(userId.value) }
            .orderBy(ImageTable.createdAt, SortOrder.DESC_NULLS_LAST)
            .limit(count)
            .map { it.toImageRecord() }
    }
}

object ImageQuery {
    val columns = with(ImageTable) {
        listOf(id, url, name, aspect, description, attribution, attributionUrl, caption, variants)
    }
}
