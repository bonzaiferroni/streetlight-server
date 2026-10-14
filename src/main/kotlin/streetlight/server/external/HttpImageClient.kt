package streetlight.server.external

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kabinet.console.globalConsole
import kampfire.model.Url
import streetlight.server.model.ImageClient
import streetlight.server.model.StreetlightAgent

/** Fetches images over HTTP as the [StreetlightAgent]. */
class HttpImageClient: ImageClient {
    private val httpClient = HttpClient(Apache5) {
        defaultRequest {
            header("User-Agent", StreetlightAgent.UserAgent)
        }
    }

    override suspend fun download(url: Url): ByteArray? {
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
}

private val console = globalConsole.getHandle(HttpImageClient::class)
