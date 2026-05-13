package streetlight.server.routes

import klutch.server.ApiContext
import klutch.server.getEndpoint
import streetlight.model.Api
import streetlight.web.doc.SiteDocTable
import streetlight.web.doc.SiteDocTree

fun ApiContext.serveSiteDocs() {

    getEndpoint(Api.Docs) {
        val docId = it.data
        SiteDocTree.nodes[docId]
    }

    getEndpoint(Api.SiteDocTable) {
        SiteDocTable
    }
}