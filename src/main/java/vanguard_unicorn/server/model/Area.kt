package vanguard_unicorn.server.model

import kotlinx.serialization.Serializable

@Serializable
data class Area(
    val id: Int,
    val name: String,
    val parentId: Int?
)