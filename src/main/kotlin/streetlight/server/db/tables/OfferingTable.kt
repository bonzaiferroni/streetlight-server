package streetlight.server.db.tables

import streetlight.model.data.ImageTarget
import klutch.db.image
import klutch.utils.transformMarkdown
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** What each star offers for sale through their seller account. */
object OfferingTable: UuidTable("offering"), ImageColumnTable {
    val starId = reference("star_id", StarTable, ReferenceOption.CASCADE).index()
    val title = text("title")
    val description = text("description").transformMarkdown().nullable()
    val tags = array<Int>("tags").nullable()
    override val image = image("image").nullable()
    override val imageTarget = ImageTarget.Offering
    val priceCents = integer("price_cents")
    val currency = text("currency")
    val platformCut = integer("platform_cut").default(0)
    val isActive = bool("is_active").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        index(null, false, tags, indexType = "GIN")
    }
}
