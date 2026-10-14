package streetlight.server.db.tables

import kampfire.model.ImageSize
import klutch.db.image
import klutch.utils.transformMarkdown
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** What each star offers for sale through their seller account. */
object OfferingTable: UuidTable("offering") {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE).index()
    val title = text("title")
    val description = text("description").transformMarkdown().nullable()
    val tags = array<Int>("tags").nullable()
    val image = image("image").nullable()
    val priceCents = integer("price_cents")
    val currency = text("currency")
    val platformCut = integer("platform_cut").default(0)
    val isActive = bool("is_active").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        index(null, false, tags, indexType = "GIN")
    }

    val imageConfig = imageConfigOf(
        table = this,
        column = image,
        ImageSize.Medium,
        ImageSize.Small,
        ImageSize.Thumb,
    )
}
