package streetlight.server.model

import klutch.server.ApiContext
import klutch.utils.UserIdentity
import streetlight.model.data.StarId
import streetlight.model.data.StarUser

typealias StreetlightRouting = ApiContext<Streetlight, StarUser, StarId>
typealias StarIdentity = UserIdentity<StarId>

val StreetlightRouting.app get() = model