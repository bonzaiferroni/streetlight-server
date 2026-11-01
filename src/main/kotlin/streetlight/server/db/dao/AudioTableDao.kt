package streetlight.server.db.dao

import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import streetlight.model.data.AudioId
import streetlight.model.data.AudioRecord
import streetlight.model.data.StarId
import streetlight.server.db.tables.AudioTable
import streetlight.server.db.tables.createAudio
import streetlight.server.db.tables.toAudio

class AudioTableDao : DbService() {

    suspend fun create(record: AudioRecord) = dbQuery {
        AudioTable.insert { it.createAudio(record) }
        record.audio
    }

    /** The audio [audioId] uploaded by [starId], or `null` when it is not theirs. */
    suspend fun readStarAudio(audioId: AudioId, starId: StarId) = dbQuery {
        AudioTable.read { it.id.eq(audioId) and it.starId.eq(starId) }.firstOrNull()?.toAudio()
    }
}
