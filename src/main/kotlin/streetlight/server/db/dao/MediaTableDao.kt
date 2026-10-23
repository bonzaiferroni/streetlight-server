package streetlight.server.db.dao

import kampfire.api.Slug
import kampfire.api.Username
import kampfire.utils.requireNotNull
import klutch.db.DbService
import klutch.db.model.CallerId
import klutch.db.read
import klutch.db.tables.nextSlugOf
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.andIfNotNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import streetlight.model.data.EntityCursor
import streetlight.model.data.Media
import streetlight.model.data.MediaEdit
import streetlight.model.data.MediaId
import streetlight.model.data.MediaType
import streetlight.server.db.tables.MediaTable
import streetlight.server.db.tables.afterCursor
import streetlight.server.db.tables.createMedia
import streetlight.server.db.tables.toMedia
import streetlight.server.db.tables.updateMedia
import streetlight.server.db.tables.SkillMediaTable
import streetlight.server.db.tables.linkSkills
import streetlight.server.db.tables.recordIdsOf
import streetlight.model.data.SkillId
import org.jetbrains.exposed.v1.core.inSubQuery
import streetlight.server.db.tables.readSkillIds
import kotlin.time.Clock

class MediaTableDao: DbService() {

    suspend fun create(edit: MediaEdit, callerId: CallerId) = dbQuery {
        val mediaId = MediaId.random()
        val slug = MediaTable.nextSlugOf(edit.title ?: mediaId.value.toString())
        val media = edit.toMedia(mediaId, slug)
        val created = MediaTable.insert {
            it.createMedia(media, callerId)
        }.resultedValues?.singleOrNull()?.toMedia()
        edit.skillIds?.let { if (created != null) SkillMediaTable.linkSkills(mediaId, it, callerId) }
        created
    }

    suspend fun update(edit: MediaEdit, callerId: CallerId) = dbQuery {
        val mediaId = edit.mediaId.requireNotNull { "mediaId missing" }
        val media = edit.toMedia(mediaId)
        val updated = MediaTable.updateReturning(where = { MediaTable.id.eq(mediaId) and MediaTable.starId.eq(callerId) } ) {
            it.updateMedia(media)
        }.singleOrNull()?.toMedia()
        edit.skillIds?.let { if (updated != null) SkillMediaTable.linkSkills(mediaId, it, callerId) }
        updated
    }

    suspend fun readSkillIds(mediaId: MediaId) = dbQuery {
        SkillMediaTable.readSkillIds(mediaId)
    }

    suspend fun readMedia(slug: Slug) = dbQuery {
        MediaTable.read { it.slug.eq(slug) }.firstOrNull()?.toMedia()
    }

    /** The page after [cursor] of media newest first, those of [username] or linked to [skillId] alone when given. */
    suspend fun readNewestMedia(cursor: EntityCursor.Time, username: Username? = null, skillId: SkillId? = null) = dbQuery {
        val after = cursor.recordAt?.let { recordAt ->
            cursor.recordId?.let { afterCursor(MediaTable.createdAt, recordAt, MediaTable.id, it, cursor.direction) }
        }
        val byStar = username?.let { MediaTable.username.eq(it) }
        val bySkill = skillId?.let { MediaTable.id.inSubQuery(SkillMediaTable.recordIdsOf(it)) }
        MediaTable.selectAll()
            .where { Op.TRUE andIfNotNull byStar andIfNotNull bySkill andIfNotNull after }
            .orderBy(MediaTable.createdAt to SortOrder.DESC, MediaTable.id to SortOrder.DESC)
            .limit(EntityCursor.DefaultLimit)
            .map { it.toMedia() }
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