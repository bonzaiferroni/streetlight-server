package streetlight.server.routes

import kampfire.api.toSlug
import kampfire.model.toOutcome
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.Entity
import streetlight.model.data.PostType
import streetlight.server.model.*

fun ApiScope.serveEntities() {
    authGate(optional = true) {
        getApi(Api.Entities.Read) { endpoint ->
            val type = readParam(endpoint.type)
            val slug = readParam(endpoint.slug).toSlug()
            val callerId = call.getIdentityOrNull()?.callerId
            val entity: Entity? = when (type) {
                PostType.Location -> dao.location.readLocation(slug, callerId)
                PostType.Media -> dao.media.readMedia(slug)
                PostType.Event -> dao.event.readEventLocationBySlug(slug, callerId)
            }
            entity.toOutcome()
        }
    }
}
