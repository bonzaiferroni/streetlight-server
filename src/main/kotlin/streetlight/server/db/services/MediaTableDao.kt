package streetlight.server.db.services

import kampfire.api.Slug
import kampfire.api.Username
import kampfire.utils.requireNotNull
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.db.read
import klutch.db.tables.nextSlugOf
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.Media
import streetlight.model.data.MediaEdit
import streetlight.model.data.MediaId
import streetlight.model.data.MediaType
import streetlight.server.db.tables.MediaTable
import streetlight.server.db.tables.createMedia
import streetlight.server.db.tables.toMedia
import streetlight.server.db.tables.updateMedia
import kotlin.time.Clock

class MediaTableDao: DbService() {

    suspend fun create(edit: MediaEdit, callerId: CallerId) = dbQuery {
        val mediaId = MediaId.random()
        val slug = MediaTable.nextSlugOf(edit.title ?: mediaId.value.toString())
        val media = edit.toMedia(mediaId, slug)
        MediaTable.insert {
            it.createMedia(media, callerId)
        }.resultedValues?.singleOrNull()?.toMedia()
    }

    suspend fun update(edit: MediaEdit, callerId: CallerId) = dbQuery {
        val mediaId = edit.mediaId.requireNotNull { "mediaId missing" }
        val media = edit.toMedia(mediaId)
        MediaTable.updateReturning(where = { MediaTable.id.eq(mediaId) and MediaTable.starId.eq(callerId) } ) {
            it.updateMedia(media)
        }.singleOrNull()?.toMedia()
    }

    suspend fun readMedia(slug: Slug) = dbQuery {
        MediaTable.read { it.slug.eq(slug) }.firstOrNull()?.toMedia()
    }

    suspend fun readMedia(username: Username, callerId: CallerId?) = dbQuery {
        // td: add mediaLight
        MediaTable.selectAll().where { MediaTable.username.eq(username) }.map { it.toMedia() }
    }
}

private fun MediaEdit.toMedia(mediaId: MediaId, slug: Slug? = null) = Media(
    mediaId = mediaId,
    slug = slug ?: Slug.Empty,
    username = Username.Empty, // set with join
    mediaType = mediaType,
    title = title,
    subtitle = subtitle,
    text = text,
    link = link,
    geoPoint = geoPoint,
    image = image,
    design = design,
    updatedAt = Clock.System.now(),
    createdAt = Clock.System.now(),
)