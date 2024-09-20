package streetlight.server.db.core

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import streetlight.server.models.User
import streetlight.model.deobfuscate
import streetlight.model.dto.AuthInfo
import streetlight.model.dto.LoginRequest
import streetlight.server.db.models.SessionToken
import streetlight.server.db.services.SessionTokenService
import streetlight.server.db.services.UserService
import streetlight.server.plugins.Log
import streetlight.server.plugins.createJWT
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

suspend fun ApplicationCall.authorize() {
    val loginRequest = this.receiveNullable<LoginRequest>() ?: return
    val userService = UserService()
    val user = userService.findByUsernameOrEmail(loginRequest.username)
    if (user == null) {
        Log.logInfo("authorize: Invalid username from ${loginRequest.username}")
        this.respond(HttpStatusCode.Unauthorized, "Invalid username")
        return
    }
    loginRequest.password?.let {
        val password = it.deobfuscate()
        val authInfo = user.testPassword(loginRequest.username, password, user.roles)
        if (authInfo == null) {
            Log.logInfo("authorize: Invalid password attempt from ${loginRequest.username}")
            return
        }
        Log.logInfo("authorize: password login by ${loginRequest.username}")
        this.respond(HttpStatusCode.OK, authInfo)
        return
    }
    loginRequest.session?.let {
        val authInfo = user.testToken(loginRequest.username, it, user.roles)
        if (authInfo == null) {
            Log.logInfo("authorize: Invalid password attempt from ${loginRequest.username}")
            this.respond(HttpStatusCode.Unauthorized, "Invalid token")
            return
        }
        Log.logInfo("authorize: session login by ${loginRequest.username}")
        this.respond(HttpStatusCode.OK, authInfo)
        return
    }
    this.respond(HttpStatusCode.Unauthorized, "Missing password or token")
}

suspend fun User.testPassword(username: String, password: String, roles: String): AuthInfo? {
    val byteArray = this.salt.base64ToByteArray()
    val hashedPassword = hashPassword(password, byteArray)
    if (hashedPassword != this.hashedPassword) {
        return null
    }

    val sessionToken = this.createSessionToken()
    val jwt = createJWT(username, roles)
    return AuthInfo(jwt, sessionToken)
}

suspend fun User.testToken(username: String, sessionToken: String, roles: String): AuthInfo? {
    val service = SessionTokenService()
    val sessionTokenEntity = service.findByToken(sessionToken)
        ?: return null
    if (sessionTokenEntity.userId != this.id) {
        return null
    }
    val jwt = createJWT(username, roles)
    return AuthInfo(jwt)
}

suspend fun User.createSessionToken(): String {
    val token = UUID.randomUUID().toString()
    val service = SessionTokenService()
    service.create(SessionToken(
        userId = this.id,
        token = token,
        createdAt = System.currentTimeMillis(),
        expiresAt = System.currentTimeMillis() + 60000,
        issuer = "http://localhost:8080/"
    ))
    return token
}

fun hashPassword(password: String, salt: ByteArray): String {
    val iterations = 65536
    val keyLength = 256
    val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val hash = factory.generateSecret(spec).encoded
    return Base64.getEncoder().encodeToString(hash)
}

fun generateSalt(): ByteArray {
    val random = SecureRandom()
    val salt = ByteArray(16)
    random.nextBytes(salt)
    return salt
}

fun ByteArray.toBase64(): String {
    return Base64.getEncoder().encodeToString(this)
}

fun String.base64ToByteArray(): ByteArray {
    return Base64.getDecoder().decode(this)
}

fun String.toSet(): Set<String> {
    return this.split(",").map { it.trim() }.toSet()
}

fun Set<String>.setToString(): String {
    return this.joinToString(",")
}
