package streetlight.server

import kampfire.api.Slug
import klutch.db.model.CallerId
import streetlight.model.data.GalaxyEdit
import streetlight.model.data.GalaxyId
import streetlight.model.data.StarId

suspend fun TestServer.seedGalaxy(founderId: StarId, slug: String = "fox-friends"): GalaxyId {
    val edit = GalaxyEdit(name = "Fox Friends", slug = Slug(slug), geoRect = DENVER_AREA, marks = emptyList())
    dao.galaxy.create(edit, CallerId(founderId.value), city = null)
    return dao.galaxy.readGalaxy(Slug(slug), null)?.galaxyId ?: error("galaxy was not created: $slug")
}
