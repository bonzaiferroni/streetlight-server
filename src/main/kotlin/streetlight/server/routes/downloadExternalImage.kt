package streetlight.server.routes

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import kabinet.console.globalConsole

private val console = globalConsole.getHandle(::downloadExternalImage.name)

suspend fun downloadExternalImage(url: String): ByteArray? {
    try {
        return httpClient.get(url).readRawBytes()
    } catch (e: Exception) {
        console.log("unable to dl image: ${e.message}")
        return null
    }
}

private val httpClient = HttpClient {
    defaultRequest {
        header("User-Agent", "Streetlight/1.0")
    }
}