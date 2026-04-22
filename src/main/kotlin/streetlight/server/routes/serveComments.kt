package streetlight.server.routes

import klutch.server.getEndpoint
import streetlight.model.Api
import streetlight.model.data.GalaxyId
import streetlight.server.model.StreetlightRouting
import streetlight.server.model.server

fun StreetlightRouting.serveComments() {
    getEndpoint(Api.Comments.ReadGalaxy, { GalaxyId(it) }) {
        server.dao.comment.readGalaxyComments(it.data)
    }
}