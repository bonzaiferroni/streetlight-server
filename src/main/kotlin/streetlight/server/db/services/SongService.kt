package streetlight.server.db.services

import streetlight.model.Song
import streetlight.server.db.DataService
import streetlight.server.db.tables.SongEntity
import streetlight.server.db.tables.UserEntity

class SongService : DataService<Song, SongEntity>(SongEntity) {
    override suspend fun createEntity(data: Song): (SongEntity.() -> Unit)? {
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            this.user = user
            name = data.name
            artist = data.artist
        }
    }

    override fun SongEntity.toData() = Song(
        id.value,
        user.id.value,
        name,
        artist
    )

    override suspend fun updateEntity(data: Song): ((SongEntity) -> Unit)? {
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            it.user = user
            it.name = data.name
            it.artist = data.artist
        }
    }
}