package streetlight.server.routes

import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import kampfire.model.toDataOrNull
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import streetlight.model.Api
import streetlight.model.ui.StarDashRoute
import streetlight.model.ui.StarDashTab
import streetlight.server.service.onboardSeller
import streetlight.server.service.readSellerStatus
import streetlight.server.service.refreshSellerLink
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity
import streetlight.server.utils.starId

fun ApiScope.serveSellers() {
    authGate {
        postApi(Api.Sellers.Onboard) {
            onboardSeller(call.getIdentity().starId, it.data)
        }

        get(Api.Sellers.Onboard.Refresh.path) {
            val url = refreshSellerLink(call.getIdentity().starId).toDataOrNull()?.value
                ?: StarDashRoute(StarDashTab.ECommerce).toRelativePath()
            call.respondRedirect(url)
        }

        getApi(Api.Sellers.ReadStatus) {
            readSellerStatus(call.getIdentity().starId)
        }
    }
}
