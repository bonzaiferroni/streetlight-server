package streetlight.server.routes

import kampfire.model.toResponse
import klutch.server.ApiContext
import klutch.server.getApi
import klutch.server.getEndpoint
import streetlight.model.Api
import streetlight.web.doc.SiteDocTable
import streetlight.web.doc.SiteDocTree

fun ApiContext.serveSiteDocs() {

    getApi(Api.Docs) {
        val docId = it.data
        SiteDocTree.nodes[docId].toResponse()
    }

    getApi(Api.SiteDocTable) {
        SiteDocTable.toResponse()
    }
}