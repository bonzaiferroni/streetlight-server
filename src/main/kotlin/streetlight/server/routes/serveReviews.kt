package streetlight.server.routes

import kampfire.model.toResponse
import klutch.server.ApiContext
import klutch.server.authGate
import klutch.server.getApi
import streetlight.model.Api
import streetlight.model.data.toRecordId
import streetlight.server.model.dao
import streetlight.server.model.getIdentity

fun ApiContext.serveReviews() {

    authGate {
        getApi(Api.Tasks.ReadStarTasks) {
            val callerId = call.getIdentity().starId
            dao.review.readCallerTasks(callerId).toResponse()
        }

        getApi(Api.Tasks.ReadStarTask, { it.toRecordId() }) {
            val reviewId = it.data
            dao.review.readQuorumReview(reviewId).toResponse()
        }
    }
}