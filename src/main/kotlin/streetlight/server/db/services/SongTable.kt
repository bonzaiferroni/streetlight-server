package streetlight.server.db.services

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object SongTable : IntIdTable() {
    val user = reference("user_id", UserTable)
    val name = text("name")
    val artist = text("artist").nullable()
}

class SongEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, SongEntity>(SongTable)

    var user by UserEntity referencedOn SongTable.user
    var name by SongTable.name
    var artist by SongTable.artist
}