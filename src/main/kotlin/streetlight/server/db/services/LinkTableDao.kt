package streetlight.server.db.services

import kampfire.model.Url
import kampfire.model.toUrl
import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.Link
import streetlight.model.data.LinkAlias
import streetlight.model.data.LinkId
import streetlight.model.data.OriginId
import streetlight.server.db.tables.LinkAliasTable
import streetlight.server.db.tables.LinkTable
import streetlight.server.db.tables.createLink
import streetlight.server.db.tables.createLinkAlias
import streetlight.server.db.tables.updateLink
import kotlin.time.Instant

class LinkTableDao: DbService() {

    suspend fun createAliasIgnore(alias: LinkAlias) = dbQuery {
        LinkAliasTable.insertIgnore {
            it.createLinkAlias(alias)
        }
    }

    suspend fun createLink(link: Link) = dbQuery {
        LinkTable.insert {
            it.createLink(link)
        }
    }

    suspend fun updateLink(link: Link) = dbQuery {
        LinkTable.update({ LinkTable.id.eq(link.linkId) }) {
            it.updateLink(link)
        }
    }

    suspend fun readLinkByAlias(url: Url) = dbQuery {
        LinkAliasTable.leftJoin(LinkTable).select(LinkTable.columns).where {
            LinkAliasTable.url.eq(url.value)
        }.singleOrNull()?.toLink()
    }

    suspend fun readLink(url: Url) = dbQuery {
        LinkTable.selectAll().where {
            LinkTable.url.eq(url.value)
        }.singleOrNull()?.toLink()
    }
}

fun ResultRow.toLink() = Link(
    linkId = LinkId(this[LinkTable.id].value),
    originId = OriginId(this[LinkTable.originId].value),
    url = this[LinkTable.url].toUrl(),
    schemaType = this[LinkTable.schemaType],
    fetchedAt = this[LinkTable.fetchedAt],
    createdAt = this[LinkTable.createdAt],
    access = this[LinkTable.access],
    content = this[LinkTable.content],
    parseOutcome = this[LinkTable.parseOutcome],
    parseNote = this[LinkTable.parseNote],
)