package streetlight.server.db.tables

import kampfire.model.ImageSize
import klutch.db.scaledImages
import klutch.db.tables.UserTable
import klutch.db.url
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import streetlight.model.data.Star

// this provides additional properties for User, likely will become the only table for account information
object StarTable: UUIDTable("star") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val description = text("description").nullable()
    val imageRef = url("image_ref").nullable()
    val images = scaledImages("images").nullable()

    val imageConfig = imageConfigOf(
        table = this,
        refColumn = imageRef,
        arrayColumn = images,
        ImageSize.Medium,
        ImageSize.Small
    )
}

fun ResultRow.toStar() = Star(
    name = this[UserTable.username],
    description = this[StarTable.description],
    imageRef = this[StarTable.imageRef],
    images = this[StarTable.images],
    updatedAt = this[UserTable.updatedAt],
    createdAt = this[UserTable.createdAt],
)