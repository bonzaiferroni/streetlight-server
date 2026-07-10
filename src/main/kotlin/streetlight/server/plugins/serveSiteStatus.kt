package streetlight.server.plugins

import kampfire.model.toOutcome
import klutch.server.getApi
import klutch.server.postApi
import klutch.server.readParam
import streetlight.model.Api
import streetlight.model.data.MetricResolution
import streetlight.server.model.ApiScope

fun ApiScope.serveSiteStatus() {
    getApi(Api.Status.Feed) {
        dao.siteStatus.readByResolution(MetricResolution.OneMinute).toOutcome()
    }

    getApi(Api.Status.ReadLast) {
        val resolution = readParam(it.resolution)
        dao.siteStatus.readByResolution(resolution, 1).firstOrNull().toOutcome()
    }
}