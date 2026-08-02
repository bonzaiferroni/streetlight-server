package streetlight.server.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.principal
import io.ktor.server.plugins.ratelimit.rateLimit
import kampfire.api.UserApi
import kampfire.api.deobfuscatePassword
import kampfire.model.AccountType
import kampfire.model.Ok
import kampfire.model.Problem
import kampfire.model.Token
import kampfire.model.toOutcome
import klutch.db.model.SessionIdentity
import klutch.db.services.SessionService
import klutch.server.Authorizer
import klutch.server.GUEST_COOKIE_NAME
import klutch.server.appendGuestCooke
import klutch.server.appendSessionCookie
import klutch.server.authGate
import klutch.server.generateToken
import klutch.server.getApi
import klutch.server.postApi
import klutch.server.provide
import streetlight.server.db.datascope.createRegisteredUser
import streetlight.server.db.datascope.requestEmailVerification
import streetlight.server.model.ApiScope
import streetlight.server.model.getIdentity
import streetlight.server.plugins.RateLimits
import streetlight.server.utils.starId

private val log = KotlinLogging.logger(ApiScope::serveSession.name)

fun ApiScope.serveSession() {
    val authorizer = provide<Authorizer>()
    val service = provide<SessionService>()

    postApi(UserApi.Create) {
        val request = it.data
        val outcome = when (request.accountType) {
            AccountType.Guest -> {
                val token = generateToken()
                when (val outcome = authorizer.createGuestUser(request, token)) {
                    is Ok -> {
                        call.appendGuestCooke(token)
                        outcome
                    }
                    is Problem -> outcome
                }
            }
            AccountType.Registered -> {
                createRegisteredUser(request, authorizer)
            }
        }

        when (outcome) {
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

    getApi(UserApi.Login.CheckGuest) {
        val outcome = when (val token = call.request.cookies[GUEST_COOKIE_NAME]) {
            null -> Ok(null)
            else -> {
                val outcome = authorizer.checkGuest(Token(token))
                if (outcome is Ok && outcome.data != null) {
                    call.appendGuestCooke(Token(token))
                }
                outcome
            }
        }
        if (outcome is Problem || outcome is Ok && outcome.data == null) {
            call.appendGuestCooke(null)
        }
        outcome
    }

    rateLimit(RateLimits.Login) {
        postApi(UserApi.Login) { request ->
            log.debug { "logging in" }
            val token = call.request.cookies[GUEST_COOKIE_NAME]?.let { Token(it) }
            when(val outcome = authorizer.authorize(request.data, token)) {
                is Ok -> {
                    val session = outcome.data
                    call.appendSessionCookie(session)
                    Ok(true)
                }
                is Problem -> outcome
            }
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
            service.readPrivateInfo(identity.username).toOutcome()
        }
    }

    authGate {
        postApi(UserApi.AccountUpgrade) {
            val identity = call.getIdentity()
            if (identity.accountType != AccountType.Guest) return@postApi Problem("Account is not a guest.")
            val request = it.data
            request.email?.let { email ->
                if (dao.star.readAccount(email) != null) return@postApi Problem("That email is already registered.")
                requestEmailVerification(identity.starId, email)
            }
            // td: send email verification if present
            when (val outcome = authorizer.upgradeAccount(
                callerId = identity.callerId,
                password = request.password.deobfuscatePassword(),
                email = request.email
            )) {
                is Ok -> {
                    call.appendGuestCooke(null)
                    outcome
                }
                else -> outcome
            }
        }
    }
}

