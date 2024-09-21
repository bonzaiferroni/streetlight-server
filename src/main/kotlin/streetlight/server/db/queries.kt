package streetlight.server.db

import org.jetbrains.exposed.sql.Transaction
import streetlight.server.db.tables.UserEntity
import streetlight.server.db.tables.UserTable

fun Transaction.findUser(username: String) = UserEntity.find { UserTable.username eq username }.firstOrNull()

fun Transaction.findUserIdOrThrow(username: String) = UserTable
    .select(UserTable.id)
    .where { UserTable.username eq username }
    .map { it[UserTable.id] }
    .firstOrNull() ?: throw IllegalArgumentException("User not found")