package streetlight.server.db.tables

import kampfire.model.BasicUser
import kampfire.model.ImageSize
import kampfire.model.UserId
import kampfire.model.UserRole
import klutch.db.scaledImages
import klutch.db.url
import klutch.utils.toStringId
import klutch.utils.toUUID
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Star
import streetlight.model.data.StarId
import streetlight.model.data.StarUser

// this provides additional properties for User, likely will become the only table for account information
object StarTable: UUIDTable("star") {
    val username = text("username")
    val hashedPassword = text("hashed_password")
    val salt = text("salt")
    val email = text("email").nullable()
    val roles = array<String>("roles")
    val name = text("name").nullable()
    val description = text("description").nullable()
    val imageRef = url("image_ref").nullable()
    val images = scaledImages("images").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    val imageConfig = imageConfigOf(
        table = this,
        refColumn = imageRef,
        arrayColumn = images,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb
    )
}

fun ResultRow.toStar() = Star(
    username = this[StarTable.username],
    roles = this[StarTable.roles].map { UserRole.valueOf(it) }.toSet(),
    name = null, // td: allow user control over publishing name
    description = this[StarTable.description],
    imageRef = this[StarTable.imageRef],
    images = this[StarTable.images],
    updatedAt = this[StarTable.updatedAt],
    createdAt = this[StarTable.createdAt],
)

fun ResultRow.toStarUser() = StarUser(
    starId = StarId(this[StarTable.id].value.toStringId()),
    username = this[StarTable.username],
    hashedPassword = this[StarTable.hashedPassword],
    salt = this[StarTable.salt],
    email = this[StarTable.email],
    roles = this[StarTable.roles].map { UserRole.valueOf(it) }.toSet(),
    createdAt = this[StarTable.createdAt],
    updatedAt = this[StarTable.updatedAt],
)

fun UpdateBuilder<*>.writeFull(user: StarUser) {
    this[StarTable.id] = user.userId.value.toUUID()
    writeUpdate(user)
}

fun UpdateBuilder<*>.writeUpdate(user: StarUser) {
    this[StarTable.username] = user.username
    this[StarTable.hashedPassword] = user.hashedPassword
    this[StarTable.salt] = user.salt
    this[StarTable.email] = user.email
    this[StarTable.roles] = user.roles.map { it.name }
    this[StarTable.createdAt] = user.createdAt
    this[StarTable.updatedAt] = user.updatedAt
}