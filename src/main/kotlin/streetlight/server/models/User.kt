package streetlight.server.models

import kotlinx.serialization.Serializable
import streetlight.model.core.IdModel
import streetlight.model.dto.PrivateInfo

@Serializable
data class User(
    override val id: Int = 0,
    val name: String? = "",
    val username: String = "",
    val hashedPassword: String = "",
    val salt: String = "",
    val email: String? = null,
    val roles: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val avatarUrl: String? = null,
    val venmo: String? = null,
): IdModel

// TODO: Move to server module

fun User.toPrivateInfo() = PrivateInfo(
    name = this.name,
    email = this.email,
)