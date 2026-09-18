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
import streetlight.model.data.GalaxyId
import streetlight.model.data.MapQuery
import streetlight.model.data.PostCursor
import streetlight.model.data.PostId
import streetlight.model.data.SortDirection
import streetlight.server.model.*

fun ApiScope.servePosts() {
    authGate(optional = true) {
        getApi(Api.Posts.ReadMapQuery) {
            val callerId = call.getIdentityOrNull()?.callerId
            val query = mapQuery() ?: return@getApi HttpProblem.BadRequest
            val posts = dao.post.readBoundedPosts(callerId, query)
            readFeedMarks(posts, callerId, query.cursor).toOk()
        }

        getApi(Api.Posts.ReadFeed) { request ->
            val galaxyId = readParamOrNull(request.galaxyId)
            val cursor = readCursor(request)
            val callerId = call.getIdentityOrNull()?.callerId
            val posts = galaxyId?.let { dao.post.readGalaxyPosts(galaxyId, callerId, cursor) }
                ?: dao.post.readHomePosts(callerId, cursor)
            Ok(readFeedMarks(posts, callerId, cursor))
        }
    }
}

private fun RoutingContext.mapQuery(): MapQuery? {
    val it = Api.Posts.ReadMapQuery
    val view = readParam(it.view) ?: return null
    val seen = readParamOrNull(it.seen)
    val postId = readParamOrNull(it.postId)?.let { PostId(it) }
    val postLean = readParamOrNull(it.postLean)
    val cursor = if (postId != null && postLean != null) PostCursor.Lean(SortDirection.Descending, postId, postLean)
    else PostCursor.Lean.Default
    return MapQuery(view, seen, cursor)
}