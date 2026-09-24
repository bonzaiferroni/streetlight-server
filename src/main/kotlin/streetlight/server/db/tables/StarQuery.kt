package streetlight.server.db.tables

import kampfire.api.toMarkdown
import kampfire.api.toUsername
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import streetlight.model.data.IdentityVisibility
import streetlight.model.data.Star

object StarQuery {
    val columns = listOf(
        StarTable.username, StarTable.roles, StarTable.accountType, StarTable.name, StarTable.identityVisibility,
        StarTable.image, StarTable.tagline, StarTable.description,
        StarTable.scoutLevel, StarTable.createdAt,
    )

    fun getColumns(withDesign: Boolean) = if (withDesign) columns + StarTable.design else columns
}

fun ResultRow.toStar() = Star(
    username = this[StarTable.username].toUsername(),
    roles = this[StarTable.roles].toRoleSet(),
    accountType = this[StarTable.accountType],
    name = getIfVisibilityPublic(StarTable.name),

    image = this[StarTable.image],
    design = getOrNull(StarTable.design),
    tagline = this[StarTable.tagline],
    description = this[StarTable.description]?.toMarkdown(),

    scoutLevel = this[StarTable.scoutLevel],
    createdAt = this[StarTable.createdAt],
)

/** The value of [column] when the star's identity is public, or `null`. */
fun <T> ResultRow.getIfVisibilityPublic(column: Column<T>): T? = when (this[StarTable.identityVisibility]) {
    IdentityVisibility.Public -> this[column]
    else -> null
}