package streetlight.server.db.services

import kampfire.api.Slug
import kampfire.api.Username
import klutch.db.DbService
import klutch.db.read
import klutch.db.tables.nextSlugOf
import klutch.utils.eq
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.Media
import streetlight.model.data.MediaEdit
import streetlight.model.data.MediaId
import streetlight.model.data.MediaType
import streetlight.model.data.StarId
import streetlight.server.db.tables.MediaTable
import streetlight.server.db.tables.createRecord
import streetlight.server.db.tables.toMedia
import kotlin.time.Clock

class MediaTableDao: DbService() {

    suspend fun createMedia(edit: MediaEdit, callerId: StarId) = dbQuery {
        val mediaId = MediaId.random()
        val slug = MediaTable.nextSlugOf(edit.title ?: mediaId.value.toString())
        val media = edit.toMedia(mediaId, slug)
        MediaTable.insert {
            it.createRecord(media, callerId)
        }
        slug
    }

    suspend fun readMedia(slug: Slug) = dbQuery {
        MediaTable.read { it.slug.eq(slug) }.firstOrNull()?.toMedia()
    }
}

private fun MediaEdit.toMedia(mediaId: MediaId, slug: Slug) = Media(
    mediaId = mediaId,
    slug = slug,
    username = Username.Empty, // set with join
    mediaType = MediaType.Text,
    title = title,
    subtitle = subtitle,
    text = text,
    link = link,
    geoPoint = geoPoint,
    image = image,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)