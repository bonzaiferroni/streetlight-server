package streetlight.server.routes

import kampfire.model.HttpProblem
import kampfire.model.Ok
import kampfire.model.toOk
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.readParamOrNull
import streetlight.model.Api
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
