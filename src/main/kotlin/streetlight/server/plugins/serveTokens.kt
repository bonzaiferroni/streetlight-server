package streetlight.server.plugins

import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.post
import kampfire.api.ActionResult
import klutch.db.services.SessionService
import streetlight.model.Api
import streetlight.model.ui.ActionReportRoute
import streetlight.server.model.ApiScope

fun ApiScope.serveTokens() {
    val sessionService = provide(SessionService::class)

    post(Api.Tokens.ResetPassword.serverIdTemplate) {
        // val email = it.data
        // requestPasswordReset(email)
    }

    post(Api.Tokens.ResetPassword.Redemption.serverIdTemplate) {
        // val request = it.data
        // redeemPasswordReset(request, sessionService)
    }

    post(Api.Tokens.AccountNotOwned.path) {
        val token = call.getTokenOrNull()
        println(token)
        call.respondRedirect(ActionReportRoute(ActionResult.Success).toRelativePath())
    }
}

// fun AppScreen.toIdPath() = "/$pathRoot/{id}"

private suspend fun RoutingCall.getTokenOrNull() = receiveParameters()["token"]