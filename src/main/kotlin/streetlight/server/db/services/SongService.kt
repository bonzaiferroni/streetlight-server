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
            music = data.music
        }
    }

    override fun SongEntity.toData() = Song(
        id.value,
        user.id.value,
        name,
        artist,
        music,
    )

    override suspend fun updateEntity(data: Song): ((SongEntity) -> Unit)? {
        val user = UserEntity.findById(data.userId) ?: return null
        return {
            it.user = user
            it.name = data.name
            it.artist = data.artist
            it.music = data.music
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
            music = data.music
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

    suspend fun readSong(id: Int, username: String): Song = dbQuery {
        val user = getUser(username)
        val entity = SongEntity.find { SongTable.id eq id }.firstOrNull() ?:
            throw IllegalArgumentException("No song found with id: $id")
        if (entity.user.id != user.id) throw IllegalArgumentException("Song does not belong to user")
        entity.toData()
    }

    suspend fun update(data: Song, username: String): Song = dbQuery {
        val user = getUser(username)
        var entity = SongEntity.find { SongTable.id eq data.id }.firstOrNull() ?:
            throw IllegalArgumentException("No song found with id: ${data.id}")
        if (entity.user.id != user.id) throw IllegalArgumentException("Song does not belong to user")
        entity = SongEntity.findByIdAndUpdate(data.id) {
            it.name = data.name
            it.artist = data.artist
            it.music = data.music
        } ?: throw IllegalArgumentException("Not found")
        entity.toData()
    }
}