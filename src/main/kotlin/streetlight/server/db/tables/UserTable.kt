package streetlight.server.db.tables

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import streetlight.model.dto.UserInfo
import streetlight.server.models.User

object UserTable : IntIdTable() {
    val name = text("name").nullable()
    val username = text("username")
    val hashedPassword = text("hashed_password")
    val salt = text("salt")
    val email = text("email").nullable()
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

fun UserEntity.toData() = User(
    this.id.value,
    this.name,
    this.username,
    this.hashedPassword,
    this.salt,
    this.email,
    this.roles,
    this.createdAt,
    this.updatedAt,
    this.avatarUrl,
    this.venmo,
)

fun UserEntity.fromData(data: User) {
    name = data.name
    username = data.username
    hashedPassword = data.hashedPassword
    salt = data.salt
    email = data.email
    roles = data.roles
    createdAt = data.createdAt
    updatedAt = data.updatedAt
    avatarUrl = data.avatarUrl
    venmo = data.venmo
}

fun UserEntity.toInfo() = UserInfo(
    this.username,
    this.roles,
    this.createdAt,
    this.updatedAt,
    this.avatarUrl,
    this.venmo,
)