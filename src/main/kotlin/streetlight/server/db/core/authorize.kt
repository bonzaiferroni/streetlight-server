package streetlight.server.db.core

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import streetlight.model.User
import streetlight.model.dto.AuthInfo
import streetlight.model.dto.LoginInfo
import streetlight.server.db.models.SessionToken
import streetlight.server.db.services.*
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.text.toCharArray

suspend fun ApplicationCall.authorize() {
    val loginInfo = this.receiveNullable<LoginInfo>() ?: return
    val user = loginInfo.getUser()
    if (user == null) {
        this.respond(HttpStatusCode.Unauthorized, "Invalid username")
        return
    }
    loginInfo.password?.let {
        val authInfo = user.testHashedPassword(it)
        if (authInfo == null) {
            this.respond(HttpStatusCode.Unauthorized, "Invalid password")
            return
        }
        this.respond(HttpStatusCode.OK, authInfo)
        return
    }
    loginInfo.session?.let {
        val authInfo = user.testSessionToken(it)
        if (authInfo == null) {
            this.respond(HttpStatusCode.Unauthorized, "Invalid token")
            return
        }
        this.respond(HttpStatusCode.OK, authInfo)
        return
    }
    this.respond(HttpStatusCode.Unauthorized, "Missing password or token")
}

suspend fun LoginInfo.getUser(): User? {
    val userService = UserService()
    if (username.contains('@')) {
        return userService.findByEmail(username)
    } else {
        return userService.findByUsername(username)
    }
}

suspend fun User.testHashedPassword(password: String): AuthInfo? {
    val byteArray = this.salt.base64ToByteArray()
    val hashedPassword = hashPassword(password, byteArray)
    if (hashedPassword != this.hashedPassword) {
        return null
    }

    val sessionToken = this.createSessionToken()
    val jwt = createJWT()
    return AuthInfo(jwt, sessionToken)
}

suspend fun User.testSessionToken(sessionToken: String): AuthInfo? {
    val service = SessionTokenService()
    val sessionTokenEntity = service.findByToken(sessionToken)
        ?: return null
    if (sessionTokenEntity.userId != this.id) {
        return null
    }
    val jwt = createJWT()
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

fun createJWT(): String {
    val audience = "http://localhost:8080/"
    val issuer = "http://localhost:8080/"
    val secret = VariableStore().appSecret
    return JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        // .withClaim("username", user.name)
        .sign(Algorithm.HMAC256(secret))
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
