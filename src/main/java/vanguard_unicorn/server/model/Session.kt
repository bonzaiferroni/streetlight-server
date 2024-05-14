package vanguard_unicorn.server.model

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class Session(
    val id: Int,
    val userId: Int,
    val locationId: Int,
    val startTime: Long,
    val duration: Duration,
    val tips: Double,
)