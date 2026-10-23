package streetlight.server.external

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kabinet.console.globalConsole
import kampfire.model.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import streetlight.server.model.ImageClient
import streetlight.server.model.StreetlightAgent
import streetlight.server.service.MAX_IMAGE_BYTES
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/**
 * Fetches images over HTTP as the [StreetlightAgent], from public addresses only.
 *
 * Each hop of a redirect is checked as the first url is, and a body past [MAX_IMAGE_BYTES] is refused unread.
 */
class HttpImageClient: ImageClient {
    private val httpClient = HttpClient(Apache5) {
        followRedirects = false
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
        }
        defaultRequest {
            header("User-Agent", StreetlightAgent.UserAgent)
        }
    }

    override suspend fun download(url: Url): ByteArray? {
        var current = url.value
        repeat(MAX_REDIRECTS + 1) {
            if (!isPublicHttpUrl(current)) {
                console.log("refused image url: $current")
                return null
            }
            val hop = try {
                fetch(current)
            } catch (e: Exception) {
                console.log("unable to dl image: ${e.message}")
                return null
            }
            when (hop) {
                is Hop.Body -> return hop.bytes
                is Hop.Redirect -> current = hop.url
                Hop.Failed -> return null
            }
        }
        console.log("too many redirects: ${url.value}")
        return null
    }

    private suspend fun fetch(url: String): Hop = httpClient.prepareGet(url).execute { response ->
        when {
            response.status.value in 300..399 -> response.headers[HttpHeaders.Location]
                ?.let { Hop.Redirect(URI(url).resolve(it).toString()) }
                ?: Hop.Failed
            !response.status.isSuccess() -> {
                console.log("unable to dl image: ${response.status}")
                Hop.Failed
            }
            (response.contentLength() ?: 0) > MAX_IMAGE_BYTES -> {
                console.log("image too large: ${response.contentLength()}")
                Hop.Failed
            }
            else -> {
                val packet = response.bodyAsChannel().readRemaining(MAX_IMAGE_BYTES + 1L)
                val bytes = packet.readByteArray()
                if (bytes.size > MAX_IMAGE_BYTES) Hop.Failed else Hop.Body(bytes)
            }
        }
    }

    private sealed interface Hop {
        class Body(val bytes: ByteArray): Hop
        class Redirect(val url: String): Hop
        data object Failed: Hop
    }
}

private const val MAX_REDIRECTS = 3

/** True when [url] is http or https and every address its host resolves to is public. */
internal suspend fun isPublicHttpUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    if (uri.scheme?.lowercase() !in setOf("http", "https")) return false
    val host = uri.host ?: return false
    val addresses = withContext(Dispatchers.IO) {
        runCatching { InetAddress.getAllByName(host).toList() }.getOrNull()
    } ?: return false
    return addresses.isNotEmpty() && addresses.all { it.isPublic() }
}

/** False for loopback, private, link-local, multicast, carrier-grade NAT, unique-local and unspecified addresses. */
private fun InetAddress.isPublic(): Boolean {
    if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress || isMulticastAddress) return false
    val bytes = address.map { it.toInt() and 0xff }
    return when (this) {
        is Inet4Address -> bytes[0] != 0 && !(bytes[0] == 100 && bytes[1] in 64..127) && bytes[0] < 224
        is Inet6Address -> (bytes[0] and 0xfe) != 0xfc
        else -> false
    }
}

private val console = globalConsole.getHandle(HttpImageClient::class)
