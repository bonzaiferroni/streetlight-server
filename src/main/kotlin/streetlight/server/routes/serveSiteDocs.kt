package streetlight.server.routes

import kampfire.model.toOutcome
import klutch.server.getApi
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.server.model.readDocContent
import streetlight.web.model.SiteDocTable
import streetlight.web.model.SiteDocTree

fun ApiScope.serveSiteDocs() {

    getApi(Api.Docs) {
        val docId = it.data
        readDocContent(docId).toOutcome()
    }

    getApi(Api.DocsTable) {
        SiteDocTable.toOutcome()
    }
}