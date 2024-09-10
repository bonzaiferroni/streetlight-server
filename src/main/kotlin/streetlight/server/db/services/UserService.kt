package streetlight.server.db.services

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import streetlight.model.core.User
import streetlight.model.core.toPrivateInfo
import streetlight.model.dto.EditUserRequest
import streetlight.model.dto.SignUpRequest
import streetlight.model.dto.UserInfo
import streetlight.server.db.DataService
import streetlight.server.db.core.generateSalt
import streetlight.server.db.core.hashPassword
import streetlight.server.db.core.toBase64
import streetlight.server.db.tables.UserEntity
import streetlight.server.db.tables.UserTable
import streetlight.server.db.tables.UserTable.username
import streetlight.server.logger
import streetlight.server.plugins.ROLE_USER

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

    fun findByUsername(username: String): User? =
        UserEntity.find { UserTable.username.lowerCase() eq username.lowercase() }.firstOrNull()?.toData()

    suspend fun findByUsernameOrEmail(usernameOrEmail: String): User? = dbQuery {
        UserEntity.find {
            (UserTable.username.lowerCase() eq usernameOrEmail.lowercase()) or
            (UserTable.email.lowerCase() eq usernameOrEmail.lowercase())
        }.firstOrNull()?.toData()
    }

    suspend fun getUserInfo(username: String): UserInfo {
        println(username)
        val user = findByUsernameOrEmail(username) ?: throw IllegalArgumentException("User not found")
        return UserInfo(
            username = user.username,
            roles = user.roles,
            avatarUrl = user.avatarUrl,
            venmo = user.venmo,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }

    suspend fun createUser(info: SignUpRequest) = dbQuery {
        validateUsername(info)
        validateEmail(info)
        validatePassword(info)
        val salt = generateSalt()

        val user = User(
            name = info.name,
            username = info.username,
            hashedPassword = hashPassword(info.password, salt),
            salt = salt.toBase64(),
            email = info.email,
            roles = ROLE_USER,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            avatarUrl = null,
            venmo = null
        )
        create(user)
    }

    private fun validateUsername(info: SignUpRequest) {
        if (!info.validUsernameLength) throw IllegalArgumentException("Username should be least 3 characters.")
        if (!info.validUsernameChars) throw IllegalArgumentException("Username has invalid characters.")
        val existingUsername = UserEntity.find { UserTable.username.lowerCase() eq info.username.lowercase() }.any()
        if (existingUsername) throw IllegalArgumentException("Username already exists.")
    }

    private fun validateEmail(info: SignUpRequest) {
        val email = info.email ?: return // email is optional
        if (!info.validEmail) throw IllegalArgumentException("Invalid email.")
        val existingEmail = UserEntity.find { UserTable.email.lowerCase() eq email.lowercase() }.any()
        if (existingEmail) throw IllegalArgumentException("Email already exists.")
    }

    private fun validatePassword(info: SignUpRequest) {
        if (!info.validPassword) throw IllegalArgumentException("Password is too weak.")
    }

    suspend fun getPrivateInfo(username: String) = dbQuery {
        val user = findByUsername(username) ?: throw IllegalArgumentException("User not found")
        user.toPrivateInfo()
    }

    suspend fun updateUser(username: String, info: EditUserRequest) = dbQuery {
        if (info.deleteUser) {
            logger.info("UserService: Deleting user $username")
            UserEntity.findSingleByAndUpdate(UserTable.username.lowerCase() eq username.lowercase()) { entity ->
                entity.delete()
            }
        } else {
            logger.info("UserService: Updating user $username")
            UserEntity.findSingleByAndUpdate(UserTable.username.lowerCase() eq username.lowercase()) { entity ->
                entity.name = info.name
                if (info.deleteName) entity.name = null
                entity.email = info.email
                if (info.deleteEmail) entity.email = null
                entity.venmo = info.venmo
                entity.avatarUrl = info.avatarUrl
                entity.updatedAt = System.currentTimeMillis()
                // TODO validate email
            }
        }
    }
}

