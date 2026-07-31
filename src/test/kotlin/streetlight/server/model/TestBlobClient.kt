package streetlight.server.model

import kampfire.model.Url
import streetlight.server.model.BlobClient

class TestBlobClient: BlobClient {

    override suspend fun put(
        bytes: ByteArray,
        filename: String,
        contentType: String
    ): Url? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(url: Url): Boolean {
        TODO("Not yet implemented")
    }
}