package streetlight.server.db.tables

import kampfire.api.PasswordHash
import kampfire.api.toEmailAddress
import kampfire.api.toUsername
import kampfire.model.AccountType
import kampfire.model.HashedToken
import kampfire.model.ImageSize
import kampfire.model.UserRecord
import kampfire.model.UserRole
import klutch.db.image
import klutch.db.jsonbConfig
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import streetlight.model.data.EmailStatus
import streetlight.model.data.IdentityVisibility
import streetlight.model.data.PageDesign
import streetlight.model.data.StarEdit
import streetlight.model.data.StarId
import kotlin.time.Clock

object StarTable: UuidTable("star") {
    // td: support hometown
    val cityId = reference("city_id", CityTable.id, onDelete = ReferenceOption.SET_NULL).nullable().index()
    val username = text("username").uniqueIndex()
    val passwordHash = text("hashed_password").nullable() // td: rename to password_hash
    val disabledPasswordHash = text("disabled_password_hash").nullable()
    val email = text("email").nullable().uniqueIndex()
    val roles = array<Int>("roles")
    val name = text("name").nullable()
    val tagline = text("tagline").nullable()
    val description = text("description").nullable()
    val identityVisibility = enumeration<IdentityVisibility>("identity_visibility").default(IdentityVisibility.Private)
    val accountType = enumeration<AccountType>("account_type")
    val emailStatus = enumeration<EmailStatus>("email_status").nullable()
    val scoutLevel = integer("scout_level").default(0)
    val stripeId = text("stripe_id").nullable()
    val venmoId = text("venmo_id").nullable()
    val paypalId = text("paypal_id").nullable()
    val cashAppId = text("cashapp_id").nullable()
    val image = image("image").nullable()
    val design = jsonb<PageDesign>("design", jsonbConfig).nullable()
    val guestToken = char("guest_token", 64).uniqueIndex().nullable()
    val activeAt = timestamp("active_at").defaultExpression(CurrentTimestamp)
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

fun List<Int>.toRoleSet() = map { ordinal -> UserRole.entries[ordinal] }.toSet()

fun ResultRow.toUserRecord() = UserRecord(
    userId = StarId(this[StarTable.id].value),
    username = this[StarTable.username].toUsername(),
    passwordHash = this[StarTable.passwordHash]?.let { PasswordHash(it) } ,
    disabledPasswordHash = this[StarTable.disabledPasswordHash]?.let { PasswordHash(it) },
    email = this[StarTable.email]?.toEmailAddress(),
    roles = this[StarTable.roles].toRoleSet(),
    accountType = this[StarTable.accountType],
    guestToken = this[StarTable.guestToken]?.let { HashedToken(it) },
    activeAt = this[StarTable.activeAt],
    createdAt = this[StarTable.createdAt],
    updatedAt = this[StarTable.updatedAt],
)

fun UpdateBuilder<*>.createUser(user: UserRecord) {
    this[StarTable.id] = user.userId.value
    this[StarTable.accountType] = user.accountType
    this[StarTable.createdAt] = user.createdAt
    updateUser(user)
}

fun UpdateBuilder<*>.updateUser(user: UserRecord) {
    this[StarTable.username] = user.username.value
    this[StarTable.passwordHash] = user.passwordHash?.value
    this[StarTable.roles] = user.roles.map { role -> role.ordinal }.toList()
    this[StarTable.guestToken] = user.guestToken?.value
    this[StarTable.updatedAt] = user.updatedAt
}

fun UpdateBuilder<*>.updateStar(edit: StarEdit) {
    this[StarTable.description] = edit.description?.value
    this[StarTable.tagline] = edit.tagline
    this[StarTable.image] = edit.image
    this[StarTable.design] = edit.design
}
