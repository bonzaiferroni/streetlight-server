package vanguard_unicorn.server.model

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val areaId: Int,
)