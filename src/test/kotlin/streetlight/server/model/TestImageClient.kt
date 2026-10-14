package streetlight.server.model

import kampfire.model.Url

class TestImageClient: ImageClient {
    override suspend fun download(url: Url): ByteArray? = TODO()
}
