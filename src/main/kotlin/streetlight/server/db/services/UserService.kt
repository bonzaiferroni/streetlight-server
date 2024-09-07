package streetlight.server.db.services

import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import streetlight.model.User
import streetlight.model.dto.UserInfo
import streetlight.server.db.DataService
import streetlight.server.db.tables.UserEntity
import streetlight.server.db.tables.UserTable

class UserService : DataService<User, UserEntity>(UserEntity) {
    override suspend fun createEntity(data: User): UserEntity.() -> Unit = {
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

    override fun UserEntity.toData() = User(
        id = this.id.value,
        name = this.name,
        username = this.username,
        hashedPassword = this.hashedPassword,
        salt = this.salt,
        email = this.email,
        roles = this.roles,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        avatarUrl = this.avatarUrl,
        venmo = this.venmo
    )

    override suspend fun updateEntity(data: User): (UserEntity) -> Unit = {
        it.name = data.name
        it.username = data.username
        it.hashedPassword = data.hashedPassword
        it.salt = data.salt
        it.email = data.email
        it.roles = data.roles
        it.createdAt = data.createdAt
        it.updatedAt = data.updatedAt
        it.avatarUrl = data.avatarUrl
        it.venmo = data.venmo
    }

    suspend fun findByUsernameOrEmail(usernameOrEmail: String): User? = dbQuery {
        UserEntity.find {
            (UserTable.username.lowerCase() eq usernameOrEmail.lowercase()) or
            (UserTable.email.lowerCase() eq usernameOrEmail.lowercase())
        }.firstOrNull()?.toData()
    }

    suspend fun getUserInfo(username: String): UserInfo {
        val user = findByUsernameOrEmail(username) ?: throw IllegalArgumentException("User not found")
        return UserInfo(
            name = user.name,
            username = user.username,
            email = user.email,
            avatarUrl = user.avatarUrl,
            venmo = user.venmo,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}

