package streetlight.server.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import streetlight.server.db.models.SessionToken

object SessionTokenTable : IntIdTable() {
    val user = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val token = text("token")
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
    val issuer = text("issuer")
}

class SessionTokenEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SessionTokenEntity>(SessionTokenTable)

    var user by UserEntity referencedOn SessionTokenTable.user
    var token by SessionTokenTable.token
    var createdAt by SessionTokenTable.createdAt
    var expiresAt by SessionTokenTable.expiresAt
    var issuer by SessionTokenTable.issuer
}

fun SessionTokenEntity.toData() = SessionToken(
    this.id.value,
    this.user.id.value,
    this.token,
    this.createdAt,
    this.expiresAt,
    this.issuer,
)

fun SessionTokenEntity.fromData(data: SessionToken) {
    user = UserEntity[data.userId]
    token = data.token
    createdAt = data.createdAt
    expiresAt = data.expiresAt
    issuer = data.issuer
}