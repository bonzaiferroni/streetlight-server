package streetlight.server.db.services

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object UserTable : IntIdTable() {
    val name = text("name")
    val email = text("email")
    val password = text("password")
}

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, UserEntity>(UserTable)

    var name by UserTable.name
    var email by UserTable.email
    var password by UserTable.password
}