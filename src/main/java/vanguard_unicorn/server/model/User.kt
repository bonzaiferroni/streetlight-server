package vanguard_unicorn.server.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val password: String,
)