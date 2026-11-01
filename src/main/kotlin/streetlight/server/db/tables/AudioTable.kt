package streetlight.server.db.tables

import klutch.db.url
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.duration
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Audio
import streetlight.model.data.AudioRecord
import streetlight.server.utils.toRecordId

object AudioTable : UuidTable("audio") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE)
    val url = url("url")
    val name = text("name").nullable()
    val description = text("description").nullable()
    val duration = duration("duration")
    val sizeBytes = long("size_bytes")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
}

fun ResultRow.toAudio() = Audio(
    audioId = toRecordId(AudioTable.id),
    url = this[AudioTable.url],
    name = this[AudioTable.name],
    description = this[AudioTable.description],
    duration = this[AudioTable.duration],
    sizeBytes = this[AudioTable.sizeBytes],
)

fun ResultRow.toAudioRecord() = AudioRecord(
    starId = this[AudioTable.starId].toRecordId(),
    audio = toAudio(),
    updatedAt = this[AudioTable.updatedAt],
    createdAt = this[AudioTable.createdAt],
)

fun UpdateBuilder<*>.createAudio(record: AudioRecord) {
    this[AudioTable.id] = record.audio.audioId.value
    this[AudioTable.starId] = record.starId.value
    this[AudioTable.url] = record.audio.url
    this[AudioTable.duration] = record.audio.duration
    this[AudioTable.sizeBytes] = record.audio.sizeBytes
    this[AudioTable.createdAt] = record.createdAt
    updateAudio(record)
}

fun UpdateBuilder<*>.updateAudio(record: AudioRecord) {
    this[AudioTable.name] = record.audio.name
    this[AudioTable.description] = record.audio.description
    this[AudioTable.updatedAt] = record.updatedAt
}
