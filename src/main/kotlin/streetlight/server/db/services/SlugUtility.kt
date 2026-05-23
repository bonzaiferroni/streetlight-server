package streetlight.server.db.services

import kampfire.api.Slug
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.ProjectId
import streetlight.server.db.tables.SlugTable
import java.text.Normalizer
import kotlin.random.Random
import kotlin.uuid.Uuid

@Deprecated("use nextSlugOf")
fun <T : Table> T.insertWithSlug(
    slugSource: String,
    slugColumn: Column<String>,
    maxAttempts: Int = 1000,
    body: T.(UpdateBuilder<*>) -> Unit
) {
    val normalizedSlug = normalizeSlugBase(slugSource)

    repeat(maxAttempts) { attempt ->
        val slug = generateSlug(normalizedSlug, attempt)

        // problem: this will fail and retry on any conflict, not just our slug. We need something more targeted.
        val statement = insertIgnore { row ->
            body(row)
            row[slugColumn] = slug
        }

        if (statement.insertedCount > 0) {
            return
        }
    }

    error("Could not generate a unique slug after $maxAttempts attempts")
}

@JvmName("nextSlugOfNullable")
fun <Id: ProjectId, T> T.nextSlugOf(
    sourceId: Id,
    sourceTable: IdTable<Uuid>,
    sourceColumn: Column<String?>,
): String  where T: Table, T: SlugTable {
    val slugRow = sourceTable.select(sourceColumn)
        .where { sourceTable.id.eq(sourceId) }
        .firstOrNull()
    if (slugRow == null) error("slug source not found")
    val slugBase = slugRow.getOrNull(sourceColumn) ?: sourceId.string

    return nextSlugOf(slugBase)
}

fun <Id: ProjectId, T> T.nextSlugOf(
    sourceId: Id,
    sourceTable: IdTable<Uuid>,
    sourceColumn: Column<String>,
): String where T: Table, T: SlugTable {
    val slugBase = sourceTable.select(sourceColumn)
        .where { sourceTable.id.eq(sourceId) }
        .firstOrNull()?.getOrNull(sourceColumn)
        ?: error("slug source not found")

    return nextSlugOf(slugBase)
}

fun <T> T.nextSlugOf(
    slugBase: String,
): String where T: Table, T: SlugTable {
    val normalizedSlug = normalizeSlugBase(slugBase)

    val exists = select(slug).where { slug.eq(normalizedSlug) }.limit(1).any()
    if (!exists) return normalizedSlug

    repeat(SLUG_MAX_ATTEMPTS) {
        val value = generateSlug(normalizedSlug, it)
        val taken = select(slug).where { slug.eq(value) }.limit(1).any()
        if (!taken) return value
    }

    error("Could not generate a unique slug after $SLUG_MAX_ATTEMPTS attempts")
}

fun generateSlug(slugBase: String, attempt: Int) = when(attempt) {
    0 -> slugBase
    else -> "$slugBase-${generateSlugSuffix()}"
}

fun normalizeSlugBase(name: String): Slug =
    Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "")
        .trim()
        .lowercase()
        .replace("\\s+".toRegex(), "-")
        .replace("[^a-z0-9\\-]".toRegex(), "")

private fun generateSlugSuffix(): String =
    buildString(SLUG_SUFFIX_LENGTH) {
        repeat(SLUG_SUFFIX_LENGTH) {
            append(SLUG_CHARS[Random.nextInt(SLUG_CHARS.length)])
        }
    }

private val SLUG_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val SLUG_SUFFIX_LENGTH = 6
private const val SLUG_MAX_ATTEMPTS = 5