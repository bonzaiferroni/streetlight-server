package streetlight.server.routes

import io.ktor.server.routing.RoutingContext
import klutch.server.readParam
import klutch.server.readParamOrNull
import streetlight.model.data.EntityCursor
import streetlight.model.data.MapEndpoint
import streetlight.model.data.MapQuery
import streetlight.model.data.SortDirection

/** The map query in the query of [MapEndpoint], or `null` without a view. */
context(context: RoutingContext)
fun MapEndpoint.mapQuery(): MapQuery? {
    val view = context.readParam(viewParam) ?: return null
    val seen = context.readParamOrNull(seenParam)
    val recordId = context.readParamOrNull(recordIdParam)
    val score = context.readParamOrNull(scoreParam)
    val tag = tagParam?.let { context.readParamOrNull(it) }
    val search = searchParam?.let { context.readParamOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }
    val cursor = EntityCursor.Score(SortDirection.Descending, recordId, score, tag, search)
    return MapQuery(view, seen, cursor)
}
