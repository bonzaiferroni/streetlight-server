package streetlight.server.db.tables

import org.jetbrains.exposed.v1.core.ColumnSet
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select

/** The expressions a query selects, each once, in the order first added. */
class SelectBuilder {
    private val expressions = linkedSetOf<Expression<*>>()

    fun add(vararg expressions: Expression<*>) {
        this.expressions.addAll(expressions)
    }

    fun addAll(expressions: Collection<Expression<*>>) {
        this.expressions.addAll(expressions)
    }

    /** The selection, as [select] takes it. */
    fun build() = expressions.toList()
}

/** Selects the expressions in [columns], then those [block] adds, from this column set. */
fun ColumnSet.selectWith(vararg columns: Collection<Expression<*>>, block: SelectBuilder.() -> Unit = {}): Query {
    val builder = SelectBuilder()
    columns.forEach { builder.addAll(it) }
    builder.block()
    return select(builder.build())
}
