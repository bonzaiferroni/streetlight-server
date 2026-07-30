package streetlight.server.routes

import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
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
import klutch.db.model.SessionIdentity
import klutch.db.services.SessionService
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.ui.ActionReportRoute
import streetlight.model.ui.Screen
import streetlight.server.db.services.changePasswordFromSession
import streetlight.server.db.services.redeemAccountLockdown
import streetlight.server.db.services.redeemAccountNotOwned
import streetlight.server.db.services.redeemPasswordReset
import streetlight.server.db.services.requestEmailVerification
import streetlight.server.db.services.requestPasswordReset
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity
import streetlight.server.utils.starId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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

        postApi(Api.AccountAction.VerifyExistingEmail) {
            val starId = call.getIdentity().starId
            val email = dao.star.readAccount(starId)?.email ?: return@postApi Problem("email not found")
            requestEmailVerification(starId, email)
        }

        getApi(Api.AccountAction.VerifyExistingEmail.CheckStatus) {
            val callerId = call.getIdentity().callerId
            dao.authToken.readIsVerifyEmailTokenActive(callerId).toOutcome()
        }

        postApi(Api.AccountAction.ChangePassword) {
            val callerId = call.getIdentity().callerId
            val sessionId = call.principal<SessionIdentity>()?.session?.sessionId ?: error("session id not found")
            changePasswordFromSession(callerId, it.data, sessionId, sessionService)
        }

        postApi(Api.AccountAction.AddEmail) {
            val email = it.data
            val starId = call.getIdentity().starId
            requestEmailVerification(starId, email)
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
        when (val outcome = redeemAccountNotOwned(token, supportAddress)) {
            is Ok -> call.respondRedirect(ActionReportRoute(ActionResult.Success).toRelativePath())
            is Problem -> {
                call.writeCookieMessage(outcome.message, Screen.ActionReport.pathBase)
                call.respondRedirect(ActionReportRoute(ActionResult.Problem).toRelativePath())
            }
        }
    }

    post(Api.AccountAction.LockdownAccount.path) {
        val token = call.receiveParameters().getToken() ?: run {
            call.respondRedirect(ActionReportRoute(ActionResult.Invalid).toRelativePath())
            return@post
        }
        when (val outcome = redeemAccountLockdown(token, sessionService, supportAddress)) {
            is Ok -> call.respondRedirect(ActionReportRoute(ActionResult.Success).toRelativePath())
            is Problem -> {
                call.writeCookieMessage(outcome.message, Screen.ActionReport.pathBase)
                call.respondRedirect(ActionReportRoute(ActionResult.Problem).toRelativePath())
            }
        }
    }
}

private fun Parameters.getToken() = this["token"]?.let { Token(it) }

fun ApplicationCall.writeCookieMessage(message: String, path: String, expiration: Duration = 1.minutes) {
    response.cookies.append(
        Cookie(
            name = MESSAGE_COOKIE_NAME,
            value = message,
            httpOnly = true,
            path = path,
            maxAge = expiration.inWholeSeconds.toInt(),
            extensions = mapOf("SameSite" to "Strict")
        )
    )
}

fun ApplicationCall.readCookieMessage(path: String): String? {
    val message = request.cookies[MESSAGE_COOKIE_NAME] ?: return null
    response.cookies.append(
        Cookie(
            name = MESSAGE_COOKIE_NAME,
            value = "",
            httpOnly = true,
            path = path,
            maxAge = 0,
            extensions = mapOf("SameSite" to "Strict")
        )
    )
    return message
}

const val MESSAGE_COOKIE_NAME = "system-message"