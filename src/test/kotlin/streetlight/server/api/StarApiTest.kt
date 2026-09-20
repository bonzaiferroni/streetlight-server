package streetlight.server.api

import kampfire.api.EmailAddress
import kampfire.api.Username
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import streetlight.model.Api
import streetlight.model.data.StarEdit
import streetlight.server.db.tables.StarTable
import streetlight.server.toDataOrThrow
import streetlight.server.toProblemOrThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StarApiTest : ApiTest() {

    @Test
    fun `a signed-out user cannot update a profile`() = runApiTest {
        registerUser("alice")

        postApi(Api.Stars.UpdateProfile, profileEdit("Hello")).toProblemOrThrow()

        assertNull(taglineOf("alice"), "a signed-out user should not change any profile")
    }

    @Test
    fun `a user updates their own profile and no one else's`() = runApiTest {
        registerUser("alice")
        registerUser("bob")
        signInAs("alice")

        postApi(Api.Stars.UpdateProfile, profileEdit("Alice was here")).toDataOrThrow()

        assertEquals("Alice was here", taglineOf("alice"), "the caller's profile should change")
        assertNull(taglineOf("bob"), "another user's profile should not change")
    }

    @Test
    fun `a profile update does not rename the user`() = runApiTest {
        registerUser("alice")
        signInAs("alice")

        postApi(Api.Stars.UpdateProfile, profileEdit("Hello").copy(username = Username("someone-else"))).toDataOrThrow()

        assertEquals("Hello", taglineOf("alice"), "the rest of the edit should apply")
        assertEquals(0, userCount("someone-else"), "the edit should not create or rename a user")
    }

    @Test
    fun `reading the account returns the account of the signed-in user`() = runApiTest {
        registerUser("alice")
        val bobId = registerUser("bob")
        signInAs("bob")

        val account = getApi(Api.Stars.ReadAccount).toDataOrThrow()

        assertEquals(bobId, account.starId)
        assertEquals(EmailAddress("bob@gmail.com"), account.email)
    }

    private fun profileEdit(tagline: String) = StarEdit(
        username = null,
        tagline = tagline,
        description = null,
        image = null,
        design = null,
    )

    private fun taglineOf(username: String) = transaction {
        StarTable.selectAll().where { StarTable.username.eq(username) }.single()[StarTable.tagline]
    }

    private fun userCount(username: String) = transaction {
        StarTable.selectAll().where { StarTable.username.eq(username) }.count().toInt()
    }
}
