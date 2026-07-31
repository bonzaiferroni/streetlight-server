package streetlight.server.model

import kampfire.model.Url

interface BlobClient {
    suspend fun put(bytes: ByteArray, filename: String, contentType: String): Url?
    suspend fun delete(url: Url): Boolean
}