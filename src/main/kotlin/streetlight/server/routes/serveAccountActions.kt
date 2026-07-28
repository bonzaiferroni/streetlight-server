package streetlight.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import kabinet.utils.Environment
import kampfire.api.ActionResult
import kampfire.api.toValidOutcome
import kampfire.model.Ok
import kampfire.model.PasswordResetRequest
import kampfire.model.Problem
import kampfire.model.Token
import kampfire.model.toOutcome
import klutch.db.services.SessionService
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.data.starId
import streetlight.model.ui.ActionReportRoute
import streetlight.server.db.services.redeemAccountNotOwned
import streetlight.server.db.services.redeemPasswordReset
import streetlight.server.db.services.requestEmailVerification
import streetlight.server.db.services.requestPasswordReset
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity

fun ApiScope.serveAccountActions() {
    val sessionService = provide(SessionService::class)
    val supportAddress = provide(Environment::class).read("STREETLIGHT_SUPPORT_ADDRESS")

    postApi(Api.AccountAction.ResetPassword) {
        val email = it.data
        requestPasswordReset(email)
    }

    authGate {
        postApi(Api.AccountAction.RemoveEmail) {
            val starId = call.getIdentity().starId
            dao.star.setEmail(starId, null, null)
            Ok(Unit)
        }

        postApi(Api.AccountAction.VerifyEmail) {
            val callerId = call.getIdentity().callerId
            requestEmailVerification(callerId)
        }

        getApi(Api.AccountAction.VerifyEmail.CheckStatus) {
            val callerId = call.getIdentity().callerId
            dao.authToken.readIsVerifyEmailTokenActive(callerId).toOutcome()
        }
    }

    // the following endpoints call post rather than postApi
    // because they are sent from static pages with minimal handling

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
        val token = call.receiveParameters().getToken() ?: run {
            call.respondRedirect(ActionReportRoute(ActionResult.Invalid).toRelativePath())
            return@post
        }
        val outcome = redeemAccountNotOwned(token, supportAddress)
        when (outcome) {
            is Ok -> call.respondRedirect(ActionReportRoute(ActionResult.Success).toRelativePath())
            is Problem -> call.respondRedirect(ActionReportRoute(ActionResult.InternalError).toRelativePath())
        }
    }
}

private fun Parameters.getToken() = this["token"]?.let { Token(it) }