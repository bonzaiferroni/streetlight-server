package streetlight.server.routes

import kampfire.model.toResponse
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.data.EditCompletion
import streetlight.model.data.toRecordId
import streetlight.server.model.ApiScope
import streetlight.server.model.dao
import streetlight.server.model.getIdentity

fun ApiScope.serveTasks() {

    authGate {
        getApi(Api.Tasks.ReadStarTasks) {
            val callerId = call.getIdentity().starId
            dao.review.readCallerTasks(callerId).toResponse()
        }

        getApi(Api.Tasks.ReadStarTask, { it.toRecordId() }) {
            val reviewId = it.data
            dao.review.readQuorumReview(reviewId).toResponse()
        }

        postApi(Api.Tasks.CompleteTask) {
            val completion = it.data
            when (completion) {
                is EditCompletion -> {

                }
            }
            true.toResponse()
        }
    }
}