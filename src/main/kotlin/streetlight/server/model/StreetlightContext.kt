package streetlight.server.model

import klutch.server.ApiContext
import klutch.utils.UserIdentity
import streetlight.model.data.StarId
import streetlight.model.data.StarUser

typealias StreetlightRouting = ApiContext<StreetlightServer, StarUser, StarId>
typealias StarIdentity = UserIdentity<StarId>

val StreetlightRouting.server get() = model