package streetlight.server.plugins

import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.post
import kampfire.api.ActionResult
import klutch.db.services.SessionService
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.ui.ActionReportRoute
import streetlight.server.db.services.requestPasswordReset
import streetlight.server.model.ApiScope

fun ApiScope.serveAccountActions() {
    val sessionService = provide(SessionService::class)

    postApi(Api.AccountAction.ResetPassword) {
        val email = it.data
        requestPasswordReset(email)
    }

    post(Api.AccountAction.ResetPassword.Redemption.serverIdTemplate) {
        // val request = it.data
        // redeemPasswordReset(request, sessionService)
    }

    post(Api.AccountAction.AccountNotOwned.path) {
        val token = call.getTokenOrNull()
        println(token)
        call.respondRedirect(ActionReportRoute(ActionResult.Success).toRelativePath())
    }
}

// fun AppScreen.toIdPath() = "/$pathRoot/{id}"

private suspend fun RoutingCall.getTokenOrNull() = receiveParameters()["token"]