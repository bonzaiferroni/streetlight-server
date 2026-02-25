package streetlight.server.routes

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readRawBytes

suspend fun downloadExternalImage(url: String): ByteArray {
    return httpClient.get(url).readRawBytes()
}

private val httpClient = HttpClient {
    defaultRequest {
        header("User-Agent", "Streetlight/1.0")
    }
}