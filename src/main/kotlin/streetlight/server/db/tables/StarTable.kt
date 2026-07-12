package streetlight.server.db.tables

import kampfire.api.toUsername
import kampfire.model.AccountType
import kampfire.model.ImageSize
import kampfire.model.UserRecord
import kampfire.model.UserRole
import klutch.db.image
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Star
import streetlight.model.data.StarEdit
import streetlight.model.data.StarId
import streetlight.model.data.StarRecord

object StarTable: UuidTable("star") {
    // td: support hometown
    val cityId = reference("city_id", CityTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val username = text("username").index()
    val hashedPassword = text("hashed_password")
    val salt = text("salt")
    val email = text("email").nullable()
    val roles = array<Int>("roles").transform(
        { it.map { ordinal -> UserRole.entries[ordinal] }.toSet() },
        { it.map { role -> role.ordinal }.toList() }
    )
    val name = text("name").nullable()
    val description = text("description").nullable()
    val accountType = enumeration<AccountType>("account_type")
    val scoutLevel = integer("scout_level").default(0)
    val image = image("image").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    val imageConfig = imageConfigOf(
        table = this,
        column = image,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb
    )
}

fun ResultRow.toStar() = Star(
    username = this[StarTable.username].toUsername(),
    roles = this[StarTable.roles],
    name = null, // td: allow user control over publishing name
    description = this[StarTable.description],
    scoutLevel = this[StarTable.scoutLevel],
    image = this[StarTable.image],
    updatedAt = this[StarTable.updatedAt],
    createdAt = this[StarTable.createdAt],
)

fun ResultRow.toUserRecord() = UserRecord(
    userId = StarId(this[StarTable.id].value),
    username = this[StarTable.username].toUsername(),
    hashedPassword = this[StarTable.hashedPassword],
    salt = this[StarTable.salt],
    email = this[StarTable.email],
    roles = this[StarTable.roles],
    createdAt = this[StarTable.createdAt],
    updatedAt = this[StarTable.updatedAt],
)

fun UpdateBuilder<*>.createRecord(user: StarRecord, accountType: AccountType) {
    this[StarTable.id] = user.userId.value
    this[StarTable.accountType] = accountType // initial value
    this[StarTable.createdAt] = user.createdAt
    updateRecord(user)
}

fun UpdateBuilder<*>.updateRecord(user: StarRecord) {
    this[StarTable.username] = user.username.value
    this[StarTable.hashedPassword] = user.hashedPassword
    this[StarTable.salt] = user.salt
    this[StarTable.email] = user.email
    this[StarTable.roles] = user.roles
    this[StarTable.updatedAt] = user.updatedAt
}

fun UpdateBuilder<*>.updateRecord(edit: StarEdit) {
    this[StarTable.name] = edit.name
    this[StarTable.description] = edit.description
    this[StarTable.image] = edit.image
}