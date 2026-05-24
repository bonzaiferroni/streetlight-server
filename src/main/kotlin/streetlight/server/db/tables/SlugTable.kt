package streetlight.server.db.tables

import kampfire.api.Slug
import kampfire.api.toSlug
import klutch.db.mapFirst
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.ProjectId
import streetlight.server.db.services.SlugRecord
import kotlin.uuid.Uuid

interface SlugTable {
    val slug: Column<String>
    val pastSlug: Column<String?>
}

fun ResultRow.toSlugRecord(slugColumn: Column<String>, pastSlugColumn: Column<String?>) = SlugRecord(
    slug = this[slugColumn].toSlug(),
    pastSlug = this[pastSlugColumn]?.toSlug(),
)