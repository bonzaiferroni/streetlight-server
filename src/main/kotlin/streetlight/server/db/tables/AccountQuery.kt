package streetlight.server.db.tables

import kampfire.api.toEmailAddress
import org.jetbrains.exposed.v1.core.ResultRow
import streetlight.model.data.Account
import streetlight.server.utils.toRecordId

object AccountQuery {
    val columns = listOf(
        StarTable.id,
        StarTable.email,
        // StarTable.name,
        // StarTable.identityVisibility,
        // StarTable.cityId,
        StarTable.accountType,
        StarTable.emailStatus,
    )
}

fun ResultRow.toAccount() = Account(
    starId = this[StarTable.id].toRecordId(),
    email = this[StarTable.email]?.toEmailAddress(),
    // name = this[StarTable.name],
    // identityVisibility = this[StarTable.identityVisibility],
    // cityId = this[StarTable.cityId]?.let { CityId(it.value) },
    accountType = this[StarTable.accountType],
    emailStatus = this[StarTable.emailStatus]
)