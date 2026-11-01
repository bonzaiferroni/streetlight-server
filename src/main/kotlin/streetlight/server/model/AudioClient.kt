package streetlight.server.model

import kotlin.time.Duration

/** Reads and encodes audio files. */
interface AudioClient {
    /** The duration of the audio in [bytes], or `null` when it is not a readable audio file. */
    suspend fun readDuration(bytes: ByteArray): Duration?

    /** The audio in [bytes] encoded as Opus in an mp4 container, or `null` when encoding fails. */
    suspend fun encode(bytes: ByteArray): ByteArray?
}
