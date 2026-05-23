package streetlight.server.db.tables

import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.ProjectId

interface SlugTable {
    val slug: Column<String>
}

fun <T, Id: ProjectId> T.readSlug(rowId: Id): String? where T: UuidTable, T: SlugTable =
    select(slug).where { id.eq(rowId) }.firstOrNull()?.getOrNull(slug)