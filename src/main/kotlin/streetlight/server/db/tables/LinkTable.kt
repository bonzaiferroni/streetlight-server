package streetlight.server.db.tables

import kampfire.model.Url
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.datetime.timestamp
import streetlight.model.data.Link
import streetlight.model.data.LinkAlias
import streetlight.model.data.LinkId
import streetlight.model.data.SchemaType
import kotlin.time.Clock
import kotlin.time.Instant

object LinkTable: UuidTable("link") {
    val originId = reference("origin_id", OriginTable, ReferenceOption.CASCADE).index()
    val url = text("url").uniqueIndex()
    val schemaType = enumeration<SchemaType>("schema_type").nullable()
    val fetchedAt = timestamp("fetched_at")
    val createdAt = timestamp("created_at")
}

object LinkAliasTable: UuidTable("link_alias") {
    val linkId = reference("link_id", LinkTable, ReferenceOption.CASCADE).index()
    val url = text("url").uniqueIndex()
    val createdAt = timestamp("created_at")
}

fun UpdateBuilder<*>.createLink(link: Link) {
    this[LinkTable.id] = link.linkId.value
    this[LinkTable.originId] = link.originId.value
    this[LinkTable.url] = link.url.value
    this[LinkTable.createdAt] = Clock.System.now()
    updateLink(link)
}

fun UpdateBuilder<*>.updateLink(link: Link) {
    this[LinkTable.fetchedAt] = link.fetchedAt
    this[LinkTable.schemaType] = link.schemaType
}

fun UpdateBuilder<*>.createLinkAlias(alias: LinkAlias) {
    this[LinkAliasTable.id] = alias.linkAliasId.value
    this[LinkAliasTable.linkId] = alias.linkId.value
    this[LinkAliasTable.url] = alias.url.value
    this[LinkAliasTable.createdAt] = Clock.System.now()
}