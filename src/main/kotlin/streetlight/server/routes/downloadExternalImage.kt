package streetlight.server.routes

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kabinet.console.globalConsole
import kampfire.model.Url
import streetlight.server.model.StreetlightAgent

private val console = globalConsole.getHandle(::downloadImage.name)

/** The bytes at [url], or `null` when the fetch fails. */
suspend fun downloadImage(url: Url): ByteArray? {
    try {
        val response = httpClient.get(url.value)
        if (!response.status.isSuccess()) {
            console.log("unable to dl image: ${response.status}")
            return null
        }
        return response.readRawBytes()
    } catch (e: Exception) {
        console.log("unable to dl image: ${e.message}")
        return null
    }
}

private val httpClient = HttpClient(Apache5) {
    defaultRequest {
        header("User-Agent", StreetlightAgent.UserAgent)
    }
}