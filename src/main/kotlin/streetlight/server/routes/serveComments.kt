package streetlight.server.routes

import klutch.server.getEndpoint
import streetlight.model.Api
import streetlight.model.data.GalaxyId
import streetlight.server.model.StreetlightRouting

fun StreetlightRouting.serveComments() {
    getEndpoint(Api.Comments.ReadGalaxy, { GalaxyId(it) }) {

    }
}