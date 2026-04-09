package streetlight.server.db.tables

import klutch.utils.toUUID
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.Song
import streetlight.model.data.SongNotation
import streetlight.server.utils.toProjectId

object SongTable : UUIDTable() {
    val starId = reference("star_id", StarTable, onDelete = ReferenceOption.CASCADE)
    val name = text("title")
    val artist = text("artist")
    val notation = jsonb<SongNotation>("notation", tableJsonDefault).nullable()
    val tempo = integer("tempo").nullable()
    val capo = integer("capo").nullable()
    val inRotation = bool("in_rotation").default(true)
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toSong() = Song(
    songId = toProjectId(SongTable.id),
    starId = toProjectId(SongTable.starId),
    title = this[SongTable.name],
    artist = this[SongTable.artist],
    notation = this[SongTable.notation],
    tempo = this[SongTable.tempo],
    capo = this[SongTable.capo],
    inRotation = this[SongTable.inRotation],
    updatedAt = this[SongTable.updatedAt],
    createdAt = this[SongTable.createdAt],
)

// Updaters
fun UpdateBuilder<*>.writeFull(song: Song) {
    this[SongTable.id] = song.songId.toUUID()
    this[SongTable.starId] = song.starId.toUUID()
    this[SongTable.createdAt] = song.createdAt
    writeUpdate(song)
}

fun UpdateBuilder<*>.writeUpdate(song: Song) {
    this[SongTable.name] = song.title
    this[SongTable.artist] = song.artist
    this[SongTable.notation] = song.notation
    this[SongTable.tempo] = song.tempo
    this[SongTable.capo] = song.capo
    this[SongTable.inRotation] = song.inRotation
    this[SongTable.updatedAt] = song.updatedAt
}

val tableJsonDefault = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}