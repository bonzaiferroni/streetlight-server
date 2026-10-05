package streetlight.server.routes

import io.ktor.server.routing.RoutingContext
import klutch.server.readParamOrNull
import streetlight.model.LeanCursorEndpoint
import streetlight.model.MarkCursorEndpoint
import streetlight.model.TimeCursorEndpoint
import streetlight.model.data.EntityCursor
import streetlight.model.data.MarkId

/** The time cursor in the query of [TimeCursorEndpoint], descending without a direction. */
context(context: RoutingContext)
fun TimeCursorEndpoint.readTimeCursor() = EntityCursor.Time(
    direction = context.readParamOrNull(directionParam) ?: EntityCursor.Default.direction,
    recordId = context.readParamOrNull(recordIdParam),
    recordAt = context.readParamOrNull(recordAtParam),
)

/** The lean cursor in the query of [LeanCursorEndpoint], descending without a direction. */
context(context: RoutingContext)
fun LeanCursorEndpoint.readLeanCursor() = EntityCursor.Lean(
    direction = context.readParamOrNull(directionParam) ?: EntityCursor.Lean.Default.direction,
    recordId = context.readParamOrNull(recordIdParam),
    postLean = context.readParamOrNull(leanParam),
)

/** The mark cursor in the query of [MarkCursorEndpoint], or `null` without a mark. */
context(context: RoutingContext)
fun MarkCursorEndpoint.readMarkCursor() = context.readParamOrNull(markIdParam)?.let {
    EntityCursor.Mark(MarkId(it), context.readParamOrNull(recordIdParam), context.readParamOrNull(countParam))
}

/** The feed cursor in the query of an endpoint that takes any kind: by mark, by lean, or by time. */
context(context: RoutingContext)
fun <T> T.readCursor(): EntityCursor where T: TimeCursorEndpoint, T: LeanCursorEndpoint, T: MarkCursorEndpoint =
    readMarkCursor() ?: if (context.readParamOrNull(leanParam) != null) readLeanCursor() else readTimeCursor()