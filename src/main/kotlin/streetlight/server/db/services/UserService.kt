package streetlight.server.db.services

import org.jetbrains.exposed.sql.lowerCase
import streetlight.model.User
import streetlight.server.db.DataService

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
        this.venmo
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

    suspend fun findByUsername(username: String): User? = dbQuery {
        UserEntity.find { UserTable.username.lowerCase() eq username.lowercase() }.firstOrNull()?.toData()
    }

    suspend fun findByEmail(email: String): User? = dbQuery {
        UserEntity.find { UserTable.email.lowerCase() eq email.lowercase() }.firstOrNull()?.toData()
    }
}

