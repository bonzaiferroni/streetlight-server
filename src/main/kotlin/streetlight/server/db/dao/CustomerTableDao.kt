package streetlight.server.db.dao

import kampfire.api.EmailAddress
import klutch.db.DbService
import klutch.utils.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import streetlight.model.data.CustomerId
import streetlight.model.data.StarId
import streetlight.server.db.tables.CustomerTable
import streetlight.server.db.tables.createCustomer
import streetlight.server.db.tables.toCustomer
import streetlight.server.model.Seller
import kotlin.time.Clock

class CustomerTableDao: DbService() {
    /** The customer [customerId] of [starId], or `null` when [starId] has no such customer. */
    suspend fun readCustomer(customerId: CustomerId, starId: StarId) = dbQuery {
        CustomerTable.selectAll().where { CustomerTable.id.eq(customerId) and CustomerTable.starId.eq(starId) }
            .singleOrNull()?.toCustomer()
    }

    /** The customer at [email] on [seller]'s provider account, or `null` when there is none. */
    suspend fun readCustomer(seller: Seller, email: EmailAddress) = dbQuery {
        CustomerTable.selectAll().where {
            CustomerTable.provider.eq(seller.provider) and
                CustomerTable.providerAccountId.eq(seller.providerAccountId) and
                CustomerTable.email.eq(email.value)
        }.singleOrNull()?.toCustomer()
    }

    suspend fun create(customerId: CustomerId, seller: Seller, providerCustomerId: String, email: EmailAddress) = dbQuery {
        CustomerTable.insertReturning {
            it.createCustomer(customerId, seller, providerCustomerId, email, Clock.System.now())
        }.single().toCustomer()
    }
}
