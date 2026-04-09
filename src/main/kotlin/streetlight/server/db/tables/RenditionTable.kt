package streetlight.server.db.tables

import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.SelfRating
import streetlight.model.data.Rendition
import streetlight.server.utils.toProjectId
import streetlight.server.utils.toUserId

object RenditionTable : UUIDTable() {
    val songId = reference("song_id", SongTable, onDelete = ReferenceOption.CASCADE)
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE)
    val notes = text("notes").nullable()
    val rating = enumeration<SelfRating>("rating").nullable()
    val createdAt = timestamp("created_at")
}

fun ResultRow.toRendition() = Rendition(
    renditionId = toProjectId(RenditionTable.id),
    songId = toProjectId(RenditionTable.songId),
    starId = toUserId(RenditionTable.starId),
    notes = this[RenditionTable.notes],
    rating = this[RenditionTable.rating],
    createdAt = this[RenditionTable.createdAt],
)

// Updaters
fun UpdateBuilder<*>.writeFull(rendition: Rendition) {
    this[RenditionTable.id] = rendition.renditionId.toUUID()
    this[RenditionTable.songId] = rendition.songId.toUUID()
    this[RenditionTable.starId] = rendition.starId.toUUID()
    writeUpdate(rendition)
}

fun UpdateBuilder<*>.writeUpdate(rendition: Rendition) {
    this[RenditionTable.notes] = rendition.notes
    this[RenditionTable.rating] = rendition.rating
    this[RenditionTable.createdAt] = rendition.createdAt
}