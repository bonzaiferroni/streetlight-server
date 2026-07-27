package streetlight.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import kampfire.api.ActionResult
import kampfire.api.toValidOutcome
import kampfire.model.Ok
import kampfire.model.PasswordResetRequest
import kampfire.model.Problem
import klutch.db.services.SessionService
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.ui.ActionReportRoute
import streetlight.server.db.services.redeemPasswordReset
import streetlight.server.db.services.requestPasswordReset
import streetlight.server.model.ApiScope

fun ApiScope.serveAccountActions() {
    val sessionService = provide(SessionService::class)

    postApi(Api.AccountAction.ResetPassword) {
        val email = it.data
        requestPasswordReset(email)
    }

    post(Api.AccountAction.ResetPassword.Redemption.path) {
        val request = call.receive<PasswordResetRequest>()
        when (val outcome = request.password.toValidOutcome()) {
            is Ok -> {
                when (val resetOutcome = redeemPasswordReset(request, sessionService)) {
                    is Ok -> call.respond(HttpStatusCode.OK)
                    is Problem -> call.respondText(resetOutcome.message, status = HttpStatusCode.BadRequest)
                }
            }
            is Problem -> call.respondText(outcome.message, status = HttpStatusCode.BadRequest)
        }
    }

    post(Api.AccountAction.AccountNotOwned.path) {
        val parameters = call.receiveParameters()
        val token = parameters["token"]
        // td
        call.respondRedirect(ActionReportRoute(ActionResult.Success).toRelativePath())
    }
}
