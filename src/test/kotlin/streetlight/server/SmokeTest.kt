package streetlight.server

import kampfire.api.EmailAddress
import kampfire.api.Password
import kampfire.api.Username
import kampfire.api.obfuscatePassword
import kampfire.model.AccountType
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.SignUpRequest
import kampfire.model.UserRole
import klutch.server.Authorizer
import kotlinx.coroutines.test.runTest
import streetlight.model.data.StarId
import streetlight.server.db.services.StarSessionService
import streetlight.server.db.services.StarTableDao
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest : DatabaseTest() {
    @Test
    fun `a star persists and is found again`() = runTest {
        val authorizer = Authorizer(StarSessionService())
        val dao = StarTableDao()
        val outcome = authorizer.createRegisteredUser(SignUpRequest(
            username = Username("dingo99"),
            password = Password("Hunter22!").obfuscatePassword(),
            email = EmailAddress("dingo99@gmail.com"),
            accountType = AccountType.Registered,
            stayLoggedIn = true,
        ), setOf(UserRole.User))
        val starId = StarId(outcome.toDataOrThrow().value)
        val found = dao.readAccount(starId)
        assertEquals(starId, found?.starId)
    }
}

fun <T> Outcome<T>.toDataOrThrow() = when (this) {
    is Ok -> this.data
    is Problem -> error("Problem: $message")
}