package streetlight.server.db.services

import klutch.utils.eq
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import streetlight.model.data.Slug
import streetlight.model.data.slugOf
import java.text.Normalizer

@Deprecated("use nextSlugOf")
fun <T : Table> T.insertWithSlug(
    slugSource: String,
    slugColumn: Column<String>,
    maxAttempts: Int = 1000,
    body: T.(UpdateBuilder<*>) -> Unit
) {
    val baseSlug = normalizedSlugOf(slugSource)

    repeat(maxAttempts) { attempt ->
        val slug = nextSlug(baseSlug, attempt)

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

fun <T : Table> T.nextSlugOf(
    slugSource: String,
    slugColumn: Column<String?>,
    maxAttempts: Int = 1000
): String {
    val baseSlug = normalizedSlugOf(slugSource)

    repeat(maxAttempts) { attempt ->
        val slug = nextSlug(baseSlug, attempt)
        val exists = select(slugColumn).where { slugColumn.eq(slug) }.limit(1).any()
        if (!exists) return slug
    }

    error("Could not generate a unique slug after $maxAttempts attempts")
}

private fun nextSlug(base: String, n: Int): String =
    when (n) {
        0 -> base
        else -> "$base-$n"
    }

fun normalizedSlugOf(name: String): Slug =
    Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "")
        .trim()
        .lowercase()
        .replace("\\s+".toRegex(), "-")
        .replace("[^a-z0-9\\-]".toRegex(), "")