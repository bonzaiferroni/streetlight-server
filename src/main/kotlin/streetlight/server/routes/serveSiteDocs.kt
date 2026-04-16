package streetlight.server.routes

import klutch.server.getEndpoint
import streetlight.model.Api
import streetlight.server.model.StreetlightRouting
import streetlight.web.doc.SiteDocTree

fun StreetlightRouting.serveSiteDocs() {

    getEndpoint(Api.Docs) {
        val docId = it.data
        SiteDocTree.nodes[docId]
    }
}