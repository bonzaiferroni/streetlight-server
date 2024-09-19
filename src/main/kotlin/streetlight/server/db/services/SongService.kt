package streetlight.server.db.services

import streetlight.model.core.Song
import streetlight.server.db.DataService
import streetlight.server.db.tables.SongEntity
import streetlight.server.db.tables.SongTable
import streetlight.server.db.tables.UserEntity
import streetlight.server.db.tables.UserTable

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

    private fun getUser(username: String) = UserEntity.find { UserTable.username eq username }.firstOrNull()
        ?: throw IllegalArgumentException("No user found with username: $username")

    suspend fun readSongs(username: String): List<Song> = dbQuery {
        val user = getUser(username)
        SongEntity.find { SongTable.userId eq user.id }.map { it.toData() }
    }

    suspend fun create(data: Song, username: String): Song = dbQuery {
        val user = getUser(username)
        val entity = SongEntity.new {
            this.user = user
            name = data.name
            artist = data.artist
        }
        entity.toData()
    }

    suspend fun delete(data: Song, username: String) = dbQuery {
        val user = getUser(username)
        if (data.userId != user.id.value) throw IllegalArgumentException("Song does not belong to user")
        val entity = SongEntity.find { SongTable.id eq data.id }.firstOrNull() ?:
            throw IllegalArgumentException("No song found with id: ${data.id}")
        entity.delete()
    }
}