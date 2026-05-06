package streetlight.server.routes

import kampfire.model.ApiResponse
import kampfire.model.Ok

fun <T> responseOf(data: T?): ApiResponse<T>? = when (data) {
    null -> null
    else -> Ok(data)
}