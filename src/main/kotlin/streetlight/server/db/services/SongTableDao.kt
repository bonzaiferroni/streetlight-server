package streetlight.server.db.services

import klutch.db.DbService
import klutch.db.read
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import streetlight.model.data.NewSong
import streetlight.model.data.RequestItem
import streetlight.model.data.Song
import streetlight.model.data.SongId
import streetlight.model.data.StarId
import streetlight.model.data.toRecordId
import streetlight.server.db.tables.RenditionTable
import streetlight.server.db.tables.SongTable
import streetlight.server.db.tables.toSong
import streetlight.server.db.tables.createSong
import streetlight.server.db.tables.updateSong

class SongTableDao: DbService() {
    suspend fun readById(songId: SongId) = dbQuery {
        SongTable.read { it.id.eq(songId) }.firstOrNull()?.toSong()
    }

    suspend fun readAllByUserId(starId: StarId) = dbQuery {
        SongTable.read { it.starId.eq(starId) }
            .map { it.toSong() }
    }

    suspend fun createSong(starId: StarId, newSong: NewSong): SongId = dbQuery {
        val now = Clock.System.now()
        SongTable.insertAndGetId {
            it.createSong(Song(
                songId = SongId.random(),
                starId = starId,
                title = newSong.title,
                artist = newSong.artist,
                tempo = null,
                capo = null,
                notation = null,
                inRotation = false,
                updatedAt = now,
                createdAt = now,
            ))
        }.value.toRecordId()
    }

    suspend fun updateSong(starId: StarId, song: Song): Boolean = dbQuery {
        SongTable.update({ SongTable.starId.eq(starId) and SongTable.id.eq(song.songId) }) {
            it.updateSong(song)
        } > 0
    }

    suspend fun readRequestItems(starId: StarId) = dbQuery {
        SongTable.leftJoin(RenditionTable, { id }, { songId })
            .select(SongTable.columns + RenditionTable.songId.count())
            .where { SongTable.starId.eq(starId) }
            .groupBy(SongTable.id)
            .orderBy(RenditionTable.songId.count(), SortOrder.DESC_NULLS_LAST)
            .map {
                RequestItem(
                    song = it.toSong(),
                    plays = it.getOrNull(RenditionTable.songId.count())?.toInt() ?: 0
                )
            }
    }
}