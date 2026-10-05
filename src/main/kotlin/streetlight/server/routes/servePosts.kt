package streetlight.server.routes

import io.ktor.server.routing.RoutingContext
import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.toOk
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.readParam
import klutch.server.readParamOrNull
import streetlight.model.Api
import streetlight.model.data.MapQuery
import streetlight.model.data.EntityCursor
import streetlight.model.data.MapEndpoint
import streetlight.model.data.PostId
import streetlight.model.data.SortDirection
import streetlight.server.model.*

fun ApiScope.servePosts() {
    authGate(optional = true) {
        getApi(Api.Posts.ReadMapQuery) {
            val callerId = call.getIdentityOrNull()?.callerId
            val query = it.mapQuery() ?: return@getApi HttpProblem.BadRequest
            val posts = dao.post.readBoundedPosts(callerId, query)
            readFeedMarks(posts, callerId, query.cursor).toOk()
        }

        getApi(Api.Posts.ReadFeed) { request ->
            val galaxyId = readParamOrNull(request.galaxyId)
            val cursor = request.readCursor()
            val callerId = call.getIdentityOrNull()?.callerId
            val posts = galaxyId?.let { dao.post.readGalaxyPosts(galaxyId, callerId, cursor) }
                ?: dao.post.readHomePosts(callerId, cursor)
            Ok(readFeedMarks(posts, callerId, cursor))
        }
    }
}

context(context: RoutingContext)
private fun MapEndpoint.mapQuery(): MapQuery? {
    val view = context.readParam(viewParam) ?: return null
    val seen = context.readParamOrNull(seenParam)
    val postId = context.readParamOrNull(recordIdParam)?.let { PostId(it) }
    val postLean = context.readParamOrNull(scoreParam)
    val cursor = if (postId != null && postLean != null) EntityCursor.Lean(SortDirection.Descending, postId.value, postLean)
    else EntityCursor.Lean.Default
    return MapQuery(view, seen, cursor)
}