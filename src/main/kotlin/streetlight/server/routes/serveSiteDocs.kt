package streetlight.server.routes

import kampfire.model.toResponse
import klutch.server.getApi
import streetlight.model.Api
import streetlight.server.model.ApiScope
import streetlight.web.doc.SiteDocTable
import streetlight.web.doc.SiteDocTree

fun ApiScope.serveSiteDocs() {

    getApi(Api.Docs) {
        val docId = it.data
        SiteDocTree.nodes[docId].toResponse()
    }

    getApi(Api.SiteDocTable) {
        SiteDocTable.toResponse()
    }
}