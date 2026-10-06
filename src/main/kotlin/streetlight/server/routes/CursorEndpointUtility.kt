package streetlight.server.routes

import io.ktor.server.routing.RoutingContext
import klutch.server.readParam
import klutch.server.readParamOrNull
import streetlight.model.MarkCursorEndpoint
import streetlight.model.ScoreCursorEndpoint
import streetlight.model.TimeCursorEndpoint
import streetlight.model.data.EntityCursor
import streetlight.model.data.MapEndpoint
import streetlight.model.data.MapQuery
import streetlight.model.data.MarkId
import streetlight.model.data.SortDirection

/** The time cursor in the query of [TimeCursorEndpoint], or its default direction without one. */
context(context: RoutingContext)
fun TimeCursorEndpoint.readTimeCursor() = EntityCursor.Time(
    direction = directionParam?.let { context.readParamOrNull(it) } ?: defaultDirection,
    recordId = context.readParamOrNull(recordIdParam),
    recordAt = context.readParamOrNull(recordAtParam),
)

/** The score cursor in the query of [ScoreCursorEndpoint], or its default direction without one. */
context(context: RoutingContext)
fun ScoreCursorEndpoint.readScoreCursor() = EntityCursor.Score(
    direction = directionParam?.let { context.readParamOrNull(it) } ?: defaultDirection,
    recordId = context.readParamOrNull(recordIdParam),
    score = context.readParamOrNull(scoreParam),
)

/** The mark cursor in the query of [MarkCursorEndpoint], or `null` without a mark. */
context(context: RoutingContext)
fun MarkCursorEndpoint.readMarkCursor() = context.readParamOrNull(markIdParam)?.let {
    EntityCursor.Mark(MarkId(it), context.readParamOrNull(recordIdParam), context.readParamOrNull(countParam))
}

/** The feed cursor in the query of an endpoint that takes any kind: by mark, by score, or by time. */
context(context: RoutingContext)
fun <T> T.readCursor(): EntityCursor where T: TimeCursorEndpoint, T: ScoreCursorEndpoint, T: MarkCursorEndpoint =
    readMarkCursor() ?: if (context.readParamOrNull(scoreParam) != null) readScoreCursor() else readTimeCursor()

/** The map query in the query of [MapEndpoint], or `null` without a view. */
context(context: RoutingContext)
fun MapEndpoint.mapQuery(): MapQuery? {
    val view = context.readParam(viewParam) ?: return null
    val seen = context.readParamOrNull(seenParam)
    val recordId = context.readParamOrNull(recordIdParam)
    val score = context.readParamOrNull(scoreParam)
    val cursor = if (recordId != null) EntityCursor.Score(SortDirection.Descending, recordId, score)
    else EntityCursor.Score.Default
    return MapQuery(view, seen, cursor)
}
