package streetlight.server.db.tables

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object SongTable : IntIdTable() {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val name = text("name")
    val artist = text("artist").nullable()
    val music = text("music").nullable()
}

class SongEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, SongEntity>(SongTable)

    var user by UserEntity referencedOn SongTable.userId
    var name by SongTable.name
    var artist by SongTable.artist
    var music by SongTable.music
}