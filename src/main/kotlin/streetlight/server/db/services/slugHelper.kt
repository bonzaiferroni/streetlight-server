package streetlight.server.db.services

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import streetlight.model.data.slugOf

fun <T : Table> T.insertWithSlug(
    slugSource: String,
    slugColumn: Column<String>,
    maxAttempts: Int = 100,
    body: T.(UpdateBuilder<*>) -> Unit
) {
    val baseSlug = slugOf(slugSource)

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

private fun nextSlug(base: String, n: Int): String =
    when (n) {
        0 -> base
        else -> "$base-$n"
    }