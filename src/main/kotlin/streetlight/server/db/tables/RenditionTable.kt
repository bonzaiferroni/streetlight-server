package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.SelfRating
import streetlight.model.data.Rendition
import streetlight.server.utils.toRecordId

object RenditionTable : UuidTable() {
    val songId = reference("song_id", SongTable, onDelete = ReferenceOption.CASCADE)
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE)
    val notes = text("notes").nullable()
    val rating = enumeration<SelfRating>("rating").nullable()
    val createdAt = timestamp("created_at")
}

fun ResultRow.toRendition() = Rendition(
    renditionId = toRecordId(RenditionTable.id),
    songId = toRecordId(RenditionTable.songId),
    starId = toRecordId(RenditionTable.starId),
    notes = this[RenditionTable.notes],
    rating = this[RenditionTable.rating],
    createdAt = this[RenditionTable.createdAt],
)

// Updaters
fun UpdateBuilder<*>.createRecord(rendition: Rendition) {
    this[RenditionTable.id] = rendition.renditionId.value
    this[RenditionTable.songId] = rendition.songId.value
    this[RenditionTable.starId] = rendition.starId.value
    updateRecord(rendition)
}

fun UpdateBuilder<*>.updateRecord(rendition: Rendition) {
    this[RenditionTable.notes] = rendition.notes
    this[RenditionTable.rating] = rendition.rating
    this[RenditionTable.createdAt] = rendition.createdAt
}