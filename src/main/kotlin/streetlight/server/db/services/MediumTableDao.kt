package streetlight.server.db.services

import kampfire.api.Slug
import kampfire.api.Username
import klutch.db.DbService
import klutch.db.read
import klutch.db.tables.nextSlugOf
import klutch.utils.eq
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.Medium
import streetlight.model.data.MediumEdit
import streetlight.model.data.MediumId
import streetlight.model.data.MediaType
import streetlight.model.data.StarId
import streetlight.server.db.tables.MediumTable
import streetlight.server.db.tables.SavedImageSet
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.toMedium
import kotlin.time.Clock

class MediumTableDao: DbService() {

    suspend fun createMedium(edit: MediumEdit, callerId: StarId, images: SavedImageSet?) = dbQuery {
        val mediumId = MediumId.random()
        val slug = MediumTable.nextSlugOf(edit.title ?: mediumId.value.toString())
        val media = edit.toMedium(mediumId, slug, images)
        MediumTable.insert {
            it.createRecord(media, callerId)
        }
        slug
    }

    suspend fun readMedium(slug: Slug) = dbQuery {
        MediumTable.read { it.slug.eq(slug) }.firstOrNull()?.toMedium()
    }
}

private fun MediumEdit.toMedium(mediumId: MediumId, slug: Slug, images: SavedImageSet?) = Medium(
    mediumId = mediumId,
    slug = slug,
    username = Username.Empty, // set with join
    mediaType = MediaType.Text,
    title = title,
    subtitle = subtitle,
    text = text,
    link = link,
    geoPoint = geoPoint,
    imageRef = images?.imageRef,
    images = images?.array,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)