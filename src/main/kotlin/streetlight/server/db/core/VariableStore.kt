package streetlight.server.db.core

import streetlight.model.core.User

class VariableStore {

    val appSecret by lazy { getEnvVariable("STREETLIGHT_APP_SECRET")}
    val admin by lazy { User(
        name = getEnvVariable("STREETLIGHT_ADMIN_NAME"),
        username = getEnvVariable("STREETLIGHT_ADMIN_USERNAME"),
        hashedPassword = getHashedPassword(),
        salt = getEnvVariable("STREETLIGHT_ADMIN_SALT"),
        email = getEnvVariable("STREETLIGHT_ADMIN_EMAIL"),
        roles = "admin",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )
    }

    private fun getEnvVariable(key: String): String {
        return System.getenv(key) ?: throw IllegalStateException("Missing environment variable: $key")
    }

    private fun getHashedPassword(): String {
        val salt = getEnvVariable("STREETLIGHT_ADMIN_SALT")
        val password = getEnvVariable("STREETLIGHT_ADMIN_PASSWORD")
        return hashPassword(password, salt.base64ToByteArray())
    }
}