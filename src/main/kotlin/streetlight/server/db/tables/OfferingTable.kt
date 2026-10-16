package streetlight.server.db.tables

import klutch.db.image
import klutch.utils.transformMarkdown
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.ImageTarget
import streetlight.model.data.Offering
import streetlight.model.data.OfferingEdit
import streetlight.model.data.OfferingId
import streetlight.model.data.OfferingTag
import streetlight.model.data.StarId
import kotlin.time.Instant
import kotlin.uuid.Uuid

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
    val isPublic = bool("is_public").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        index(null, false, tags, indexType = "GIN")
    }
}

fun ResultRow.toOffering() = Offering(
    offeringId = OfferingId(this[OfferingTable.id].value),
    title = this[OfferingTable.title],
    priceCents = this[OfferingTable.priceCents],
    currency = this[OfferingTable.currency],
    platformCut = this[OfferingTable.platformCut],
    isPublic = this[OfferingTable.isPublic],
    description = this[OfferingTable.description],
    tags = this[OfferingTable.tags]?.map { OfferingTag.entries[it] },
    image = this[OfferingTable.image],
)

fun UpdateBuilder<*>.createOffering(starId: StarId, edit: OfferingEdit, currency: String, now: Instant) {
    this[OfferingTable.id] = Uuid.random()
    this[OfferingTable.starId] = starId.value
    this[OfferingTable.currency] = currency
    this[OfferingTable.createdAt] = now
    updateOffering(edit, now)
}

fun UpdateBuilder<*>.updateOffering(edit: OfferingEdit, now: Instant) {
    this[OfferingTable.title] = edit.title ?: error("title not found")
    this[OfferingTable.description] = edit.description
    this[OfferingTable.tags] = edit.tags.takeIf { it.isNotEmpty() }?.map { it.ordinal }
    this[OfferingTable.image] = edit.image
    this[OfferingTable.priceCents] = edit.priceCents ?: error("price not found")
    this[OfferingTable.platformCut] = edit.platformCut
    this[OfferingTable.updatedAt] = now
}
