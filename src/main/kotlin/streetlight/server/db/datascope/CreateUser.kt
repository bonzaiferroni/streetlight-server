package streetlight.server.db.datascope

import kampfire.api.TableUuid
import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import kampfire.model.SignUpRequest
import kampfire.model.UserRole
import klutch.server.Authorizer
import streetlight.model.data.StarId
import streetlight.server.model.DataScope

/** Creates a registered user and asks them to verify their email. Fails when the email is registered already. */
suspend fun DataScope.createRegisteredUser(request: SignUpRequest, authorizer: Authorizer): Outcome<TableUuid> {
    request.email?.let { email ->
        if (dao.star.readAccount(email) != null) return Problem("That email is already registered.")
    }
    return when (val outcome = authorizer.createRegisteredUser(request, setOf(UserRole.User))) {
        is Ok -> {
            request.email?.let { email ->
                requestEmailVerification(StarId(outcome.data.value), email)
            }
            outcome
        }
        is Problem -> outcome
    }
}