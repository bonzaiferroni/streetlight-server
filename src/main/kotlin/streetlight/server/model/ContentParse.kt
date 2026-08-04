package streetlight.server.model

import kotlinx.serialization.Serializable

@Serializable
data class ContentParse<T>(
    val isExpectedContent: Boolean,
    val content: T?,
)