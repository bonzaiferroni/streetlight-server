package streetlight.server.db.dao

import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import streetlight.model.data.PaymentProvider
import streetlight.model.data.SellerConfiguration
import streetlight.model.data.StarId
import streetlight.server.db.tables.SellerTable
import streetlight.server.db.tables.createSeller
import streetlight.server.db.tables.toSeller
import streetlight.server.db.tables.updateSeller
import streetlight.server.model.ProviderAccount
import kotlin.time.Clock

class SellerTableDao: DbService() {
    suspend fun readSeller(starId: StarId, provider: PaymentProvider) = dbQuery {
        SellerTable.selectAll()
            .where { SellerTable.starId.eq(starId) and SellerTable.provider.eq(provider) }
            .singleOrNull()?.toSeller()
    }

    /** Whether [accountId] belongs to a seller. */
    suspend fun isSeller(provider: PaymentProvider, accountId: String) = dbQuery {
        SellerTable.select(SellerTable.starId)
            .where { SellerTable.provider.eq(provider) and SellerTable.providerAccountId.eq(accountId) }
            .limit(1).any()
    }

    suspend fun create(
        starId: StarId,
        provider: PaymentProvider,
        configuration: SellerConfiguration,
        account: ProviderAccount,
    ) = dbQuery {
        SellerTable.insert { it.createSeller(starId, provider, configuration, account, Clock.System.now()) }
    }

    /** Writes the flags of [account] to its seller, returning the count of rows updated. */
    suspend fun update(provider: PaymentProvider, account: ProviderAccount) = dbQuery {
        SellerTable.update({
            SellerTable.provider.eq(provider) and SellerTable.providerAccountId.eq(account.accountId)
        }) { it.updateSeller(account, Clock.System.now()) }
    }

    suspend fun delete(provider: PaymentProvider, accountId: String) = dbQuery {
        SellerTable.deleteWhere {
            SellerTable.provider.eq(provider) and SellerTable.providerAccountId.eq(accountId)
        }
    }
}
