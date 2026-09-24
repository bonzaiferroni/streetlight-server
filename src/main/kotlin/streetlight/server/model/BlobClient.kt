package streetlight.server.model

import kampfire.model.Url

/** Stores uploaded files and serves them by URL. */
interface BlobClient {
    /** Stores [bytes] as [filename] and returns its URL, or `null` when storing fails. */
    suspend fun put(bytes: ByteArray, filename: String, contentType: String): Url?
    suspend fun delete(url: Url): Boolean
}