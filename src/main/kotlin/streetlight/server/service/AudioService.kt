package streetlight.server.service

import kampfire.model.Ok
import kampfire.model.Outcome
import kampfire.model.Problem
import klutch.db.model.CallerId
import streetlight.model.data.Audio
import streetlight.model.data.AudioId
import streetlight.model.data.AudioRecord
import streetlight.model.data.MediaEdit
import streetlight.model.data.MediaType
import streetlight.server.model.DataScope
import streetlight.server.utils.toStarId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/** The most bytes of an audio file the server takes in. */
const val MAX_AUDIO_BYTES = 64 * 1024 * 1024
/** The longest audio the server encodes. */
val MAX_AUDIO_DURATION = 30.minutes

private const val AUDIO_CONTENT_TYPE = "audio/mp4"
private const val AUDIO_EXT = "m4a"

/** Encodes [bytes] as Opus in an mp4 container, stores the file, and records the audio as [callerId]'s, named [name]. */
suspend fun DataScope.encodeAudioAndStore(
    bytes: ByteArray,
    callerId: CallerId,
    name: String?,
): Outcome<Audio> {
    if (bytes.size > MAX_AUDIO_BYTES) return AudioProblem.TooLarge
    val sourceDuration = client.audio.readDuration(bytes) ?: return AudioProblem.InvalidFormat
    if (sourceDuration > MAX_AUDIO_DURATION) return AudioProblem.TooLong

    val encoded = client.audio.encode(bytes) ?: return AudioProblem.Encoding
    val duration = client.audio.readDuration(encoded) ?: return AudioProblem.Encoding

    val audioId = AudioId.random()
    val filename = "$audioId.$AUDIO_EXT"
    val url = client.blob.put(encoded, filename, AUDIO_CONTENT_TYPE) ?: return AudioProblem.Saving
    log("saved remote audio: $filename")

    val now = Clock.System.now()
    val audio = Audio(
        audioId = audioId,
        url = url,
        name = name,
        description = null,
        duration = duration,
        sizeBytes = encoded.size.toLong(),
    )
    return Ok(dao.audio.create(AudioRecord(callerId.toStarId(), audio, now, now)))
}

/**
 * The [edit] with its audio as stored: for an audio post, the [Audio] its meta names, which must be [callerId]'s, as
 * its meta and its url. Any other post is returned with no meta.
 */
suspend fun DataScope.checkMediaAudio(callerId: CallerId, edit: MediaEdit): Outcome<MediaEdit> {
    if (edit.mediaType != MediaType.Audio) return Ok(edit.copy(meta = null))
    val audioId = (edit.meta as? Audio)?.audioId ?: return AudioProblem.Missing
    val audio = dao.audio.readStarAudio(audioId, callerId.toStarId()) ?: return AudioProblem.Missing
    return Ok(edit.copy(url = audio.url, meta = audio))
}

object AudioProblem {
    val Encoding = Problem("Unable to encode audio.")
    val Saving = Problem("Unable to save audio.")
    val InvalidFormat = Problem("Invalid audio format.")
    val TooLarge = Problem("Audio file is too large.")
    val TooLong = Problem("Audio is too long.")
    val Missing = Problem("Audio not found.")
}
