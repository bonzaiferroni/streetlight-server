package streetlight.server.db.tables

import kampfire.api.toEmail
import org.jetbrains.exposed.v1.core.ResultRow
import streetlight.model.data.CityId
import streetlight.model.data.IdentityInfo

object IdentityQuery {
    val columns = listOf(
        StarTable.name, StarTable.email, StarTable.identityVisibility, StarTable.cityId
    )
}

fun ResultRow.toIdentityInfo() = IdentityInfo(
    email = this[StarTable.email]?.toEmail(),
    name = this[StarTable.name],
    identityVisibility = this[StarTable.identityVisibility],
    cityId = this[StarTable.cityId]?.let { CityId(it.value) }
)