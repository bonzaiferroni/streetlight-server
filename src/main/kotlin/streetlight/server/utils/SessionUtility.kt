package streetlight.server.utils

import klutch.db.model.CallerId
import klutch.db.model.Identity
import streetlight.model.data.StarId

fun CallerId.toStarId() = StarId(value)
val Identity.starId get() = callerId.toStarId()