package streetlight.server.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.post
import io.ktor.util.date.GMTDate
import kampfire.api.UserApi
import kampfire.model.AuthLegacy
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.Session
import kampfire.model.UserRole
import kampfire.model.outcomeOf
import kampfire.model.toOutcome
import klutch.db.services.SessionService
import klutch.server.Authorizer
import klutch.server.InvalidLoginException
import klutch.server.SESSION_COOKIE_NAME
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
                call.appendCookies(session)
                Ok(true)
            }
            is Problem -> outcome
        }
    }

    getApi(UserApi.GenerateUsername) {
        Ok(service.generateUsername())
    }

    postApi(UserApi.CheckUsername) {
        Ok(service.readByUsernameOrEmail(it.data.value) == null)
    }

    postApi(UserApi.Login) {
        log.debug { "logging in" }
        when(val outcome = authorizer.authorize(it.data)) {
            is Ok -> {
                val session = outcome.data
                call.appendCookies(session)
                Ok(true)
            }
            is Problem -> outcome
        }
    }

    authGate(true) {
        postApi(UserApi.Logout) {
            val identity = call.getIdentity()
            service.deleteSession(identity.token)
            call.response.cookies.append(
                Cookie(
                    name = SESSION_COOKIE_NAME,
                    value = "",
                    httpOnly = true,
                    secure = true,
                    path = "/",
                    expires = GMTDate.START,
                    extensions = mapOf("SameSite" to "Strict")
                )
            )
            Ok(true)
        }

        getApi(UserApi.Private) {
            val identity = call.getIdentity()
            outcomeOf(service.readPrivateInfo(identity.username))
        }
    }
}

private fun RoutingCall.appendCookies(session: Session) {
    response.cookies.append(
        Cookie(
            name = SESSION_COOKIE_NAME,
            value = session.token.value,
            httpOnly = true,
            secure = true,
            path = "/",
            maxAge = session.maxAgeSeconds,
            extensions = mapOf("SameSite" to "Strict")
        )
    )
}