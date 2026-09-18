package streetlight.server.plugins

import kampfire.model.Ok
import kampfire.model.toOutcome
import klutch.server.getApi
import klutch.server.postApi
import klutch.server.readParam
import streetlight.model.Api
import streetlight.model.data.MetricResolution
import streetlight.model.data.SiteStatusFeed
import streetlight.server.model.ApiScope

fun ApiScope.serveSiteStatus() {
    getApi(Api.Status.Feed) {
        val resolution = readParam(it.resolution)
        val points = dao.siteStatus.readByResolution(resolution)
        val events = dao.siteStatus.readEvents(resolution)
        Ok(SiteStatusFeed(points = points, events = events))
    }

    getApi(Api.Status.ReadLast) {
        val resolution = readParam(it.resolution)
        dao.siteStatus.readByResolution(resolution, 1).firstOrNull().toOutcome()
    }
}