package streetlight.server.model

import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingCall
import klutch.server.ApiContext
import klutch.utils.UserIdentity
import streetlight.model.data.StarId
import streetlight.model.data.StarUser

typealias StreetlightRouting = ApiContext<StreetlightServer>

val StreetlightRouting.server get() = model
val StreetlightRouting.dao get() = model.dao
val StreetlightRouting.service get() = model.service
