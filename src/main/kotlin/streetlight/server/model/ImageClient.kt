package streetlight.server.model

import kampfire.model.Url

/** Fetches images from other sites. */
interface ImageClient {
    /** The bytes at [url], or `null` when the fetch fails. */
    suspend fun download(url: Url): ByteArray?
}
