package streetlight.server.db.tables

import kampfire.api.toUsername
import kampfire.model.AccountType
import kampfire.model.ImageSize
import kampfire.model.UserRole
import klutch.db.scaledImages
import klutch.db.url
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
    val username = text("username")
    val hashedPassword = text("hashed_password")
    val salt = text("salt")
    val email = text("email").nullable()
    val roles = array<String>("roles")
    val name = text("name").nullable()
    val description = text("description").nullable()
    val accountType = enumeration<AccountType>("account_type")
    val scoutLevel = integer("scout_level").default(0)
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
    username = this[StarTable.username].toUsername(),
    roles = this[StarTable.roles].map { UserRole.valueOf(it) }.toSet(),
    name = null, // td: allow user control over publishing name
    description = this[StarTable.description],
    scoutLevel = this[StarTable.scoutLevel],
    imageRef = this[StarTable.imageRef],
    images = this[StarTable.images],
    updatedAt = this[StarTable.updatedAt],
    createdAt = this[StarTable.createdAt],
)

fun ResultRow.toStarUser() = StarRecord(
    starId = StarId(this[StarTable.id].value),
    username = this[StarTable.username].toUsername(),
    hashedPassword = this[StarTable.hashedPassword],
    salt = this[StarTable.salt],
    email = this[StarTable.email],
    roles = this[StarTable.roles].map { UserRole.valueOf(it) }.toSet(),
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
    this[StarTable.roles] = user.roles.map { it.name }
    this[StarTable.updatedAt] = user.updatedAt
}

fun UpdateBuilder<*>.updateRecord(edit: StarEdit, images: SavedImageSet?) {
    this[StarTable.name] = edit.name
    this[StarTable.description] = edit.description
    writeImages(StarTable.imageConfig, images)
}