package streetlight.server.db.tables

import kampfire.api.toSlug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.LocationConfig
import streetlight.model.data.LocationConfigContent
import streetlight.server.utils.toRecordId

object LocationConfigQuery {
    val columns = listOf(
        LocationTable.id,
        LocationTable.parseMode,
        LocationTable.design,
        SubdomainTable.slug,
    )

    val contentColumns = (columns + LocationAspect.columns).distinct()
}

fun locationConfigContentQuery() = LocationTable
    .leftJoin(SubdomainTable)
    .select(LocationConfigQuery.contentColumns)

fun ResultRow.toLocationConfigContent() = LocationConfigContent(
    location = toLocation(),
    config = toLocationConfig(),
)

fun ResultRow.toLocationConfig() = LocationConfig(
    locationId = toRecordId(LocationTable.id),
    parseMode = this[LocationTable.parseMode],
    design = this[LocationTable.design],
    subdomain = getOrNull(SubdomainTable.slug)?.toSlug()
)

