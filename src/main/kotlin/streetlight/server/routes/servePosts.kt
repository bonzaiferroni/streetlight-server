package streetlight.server.routes

import streetlight.server.service.readFeedMarks
import kampfire.model.HttpProblem
import kampfire.model.toOk
import klutch.server.authGate
import klutch.server.getApi
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
    }
}
