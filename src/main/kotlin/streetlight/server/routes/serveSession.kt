package streetlight.server.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import kampfire.api.UserApi
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.SessionIdentity
import kampfire.model.UserRole
import kampfire.model.outcomeOf
import klutch.db.services.SessionService
import klutch.server.Authorizer
import klutch.server.appendSessionCookie
import klutch.server.authGate
import klutch.server.getApi
import klutch.server.postApi
import klutch.server.provide
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity

private val log = KotlinLogging.logger(ApiScope::serveSession.name)

fun ApiScope.serveSession() {
    val authorizer = provide<Authorizer>()
    val service = provide<SessionService>()

    postApi(UserApi.Create) {
        when (val outcome = authorizer.createUser(it.data, setOf(UserRole.User))) {
            is Ok -> {
                val authId = outcome.data
                val session = authorizer.authorizeNewAccount(authId, it.data.stayLoggedIn)
                call.appendSessionCookie(session)
                Ok(true)
            }
            is Problem -> outcome
        }
    }

    getApi(UserApi.GenerateUsername) {
        Ok(service.generateUsername())
    }

    postApi(UserApi.CheckUsernameExists) {
        Ok(service.checkUsernameExists(it.data))
    }

    postApi(UserApi.Login) {
        log.debug { "logging in" }
        when(val outcome = authorizer.authorize(it.data)) {
            is Ok -> {
                val session = outcome.data
                call.appendSessionCookie(session)
                Ok(true)
            }
            is Problem -> outcome
        }
    }

    authGate(true) {
        postApi(UserApi.Logout) {
            log.debug { "logging out" }
            val principal = call.principal<SessionIdentity>() ?: error("principal not found")
            service.deleteSession(principal.session.token)
            call.appendSessionCookie(null)
            Ok(true)
        }

        getApi(UserApi.Private) {
            val identity = call.getIdentity()
            outcomeOf(service.readPrivateInfo(identity.username))
        }
    }
}

