package streetlight.server.db.services

import kampfire.api.Slug
import kampfire.api.normalizeSlugSource
import kampfire.api.toSlug
import klutch.db.mapFirst
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.ProjectId
import streetlight.server.db.tables.SlugTable
import streetlight.server.db.tables.toSlugRecord
import java.text.Normalizer
import kotlin.random.Random
import kotlin.uuid.Uuid

fun <T> T.readSlugRecord(recordId: ProjectId) where T: IdTable<Uuid>, T: SlugTable =
    select(slug, pastSlug).where { id.eq(recordId) }.mapFirst { it.toSlugRecord(slug, pastSlug) }

fun <T> T.readSlug(recordId: ProjectId) where T: IdTable<Uuid>, T: SlugTable =
    select(slug).where { id.eq(recordId) }.mapFirst(slug) { it.toSlug() }

fun <T> T.isSlugAvailable(value: Slug): Boolean where T: Table, T: SlugTable =
    select(slug).where { slug.eq(value) }.limit(1).none()

@JvmName("nextSlugOfNullable")
fun <Id: ProjectId, T> T.nextSlugOf(
    sourceId: Id,
    sourceTable: IdTable<Uuid>,
    sourceColumn: Column<String?>,
): Slug where T: Table, T: SlugTable {
    val slugRow = sourceTable.select(sourceColumn)
        .where { sourceTable.id.eq(sourceId) }
        .firstOrNull()
    if (slugRow == null) error("slug source not found")
    val slugBase = slugRow.getOrNull(sourceColumn) ?: return sourceId.toSlug()

    return nextSlugOf(slugBase)
}

fun <Id: ProjectId, T> T.nextSlugOf(
    sourceId: Id,
    sourceTable: IdTable<Uuid>,
    sourceColumn: Column<String>,
): Slug where T: Table, T: SlugTable {
    val slugBase = sourceTable.select(sourceColumn)
        .where { sourceTable.id.eq(sourceId) }
        .firstOrNull()?.getOrNull(sourceColumn)
        ?: error("slug source not found")

    return nextSlugOf(slugBase)
}

fun <T> T.nextSlugOf(
    slugBase: String,
): Slug where T: Table, T: SlugTable {
    val normalizedSlug = normalizeSlugBase(slugBase)

    repeat(SLUG_MAX_ATTEMPTS) {
        val slug = generateSlug(normalizedSlug, it)
        if (isSlugAvailable(slug)) return slug
    }

    error("Could not generate a unique slug after $SLUG_MAX_ATTEMPTS attempts")
}

fun <T> T.getSlugRecord(recordId: ProjectId, slugBase: String): SlugRecord where T: IdTable<Uuid>, T: SlugTable {
    val slugBase = normalizeSlugBase(slugBase)

    val record = readSlugRecord(recordId)

    if (record.slug.hasBase(slugBase)) return record

    val slug = nextSlugOf(slugBase)
    return SlugRecord(slug, record.slug)
}

fun <T> T.getDefinedSlugRecord(recordId: ProjectId, slug: Slug): SlugRecord where T: IdTable<Uuid>, T: SlugTable {
    val record = readSlugRecord(recordId)

    return if (record.slug == slug) record else SlugRecord(slug, record.slug)
}

private fun generateSlug(slugBase: String, attempt: Int) = when(attempt) {
    0 -> when (val uuid = Uuid.parseOrNull(slugBase)) {
        null -> slugBase.toSlug()
        else -> uuid.toSlug() // ensures slug will not have uuid shape
    }
    else -> "$slugBase-${generateSlugSuffix()}".toSlug()
}

private fun normalizeSlugBase(source: String): String =
    Normalizer.normalize(source, Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "")
        .normalizeSlugSource()

private fun generateSlugSuffix(): String =
    buildString(SLUG_SUFFIX_LENGTH) {
        repeat(SLUG_SUFFIX_LENGTH) {
            append(SLUG_CHARS[Random.nextInt(SLUG_CHARS.length)])
        }
    }

fun ProjectId.toSlug() = value.toSlug()

fun Uuid.toSlug() = toString().replace("-", "").toSlug()

private val SLUG_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
private const val SLUG_SUFFIX_LENGTH = 6
private const val SLUG_MAX_ATTEMPTS = 5

data class SlugRecord(val slug: Slug, val pastSlug: Slug? = null)

