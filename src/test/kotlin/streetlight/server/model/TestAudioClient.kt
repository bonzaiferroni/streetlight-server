package streetlight.server.model

import kotlin.time.Duration

class TestAudioClient: AudioClient {
    override suspend fun readDuration(bytes: ByteArray): Duration? = TODO()
    override suspend fun encode(bytes: ByteArray): ByteArray? = TODO()
}
