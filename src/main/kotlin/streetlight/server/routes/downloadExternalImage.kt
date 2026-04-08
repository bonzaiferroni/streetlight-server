package streetlight.server.routes

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import kabinet.console.globalConsole
import kampfire.model.Url

private val console = globalConsole.getHandle(::downloadImage.name)

suspend fun downloadImage(url: Url): ByteArray? {
    try {
        return httpClient.get(url.value).readRawBytes()
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