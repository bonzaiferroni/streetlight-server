package streetlight.server.routes

import kampfire.api.toSlug
import kampfire.model.toOutcome
import klutch.server.*
import streetlight.model.Api
import streetlight.model.data.Entity
import streetlight.model.data.EntityType
import streetlight.server.model.*

fun ApiScope.serveEntities() {
    authGate(optional = true) {
        getApi(Api.Entities.Read) { endpoint ->
            val type = readParam(endpoint.type)
            val slug = readParam(endpoint.slug).toSlug()
            val callerId = call.getIdentityOrNull()?.callerId
            val entity: Entity? = when (type) {
                EntityType.Location -> dao.location.readLocation(slug, callerId)
                EntityType.Media -> dao.media.readMedia(slug)
                EntityType.Event -> dao.event.readEventLocationBySlug(slug, callerId)
            }
            entity.toOutcome()
        }
    }
}
