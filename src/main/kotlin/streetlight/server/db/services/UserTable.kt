package streetlight.server.db.services

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object UserTable : IntIdTable() {
    val name = text("name")
    val username = text("username")
    val hashedPassword = text("hashed_password")
    val salt = text("salt")
    val email = text("email")
    val roles = text("roles").default("user")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val avatarUrl = text("avatar_url").nullable()
    val venmo = text("venmo").nullable()
}

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : EntityClass<Int, UserEntity>(UserTable)

    var name by UserTable.name
    var username by UserTable.username
    var hashedPassword by UserTable.hashedPassword
    var salt by UserTable.salt
    var email by UserTable.email
    var roles by UserTable.roles
    var createdAt by UserTable.createdAt
    var updatedAt by UserTable.updatedAt
    var avatarUrl by UserTable.avatarUrl
    var venmo by UserTable.venmo
}