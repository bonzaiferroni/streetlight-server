package streetlight.server.external

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import streetlight.server.model.AudioClient
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val logger = KotlinLogging.logger {}

private const val OPUS_BITRATE = "96k"
/** The most bits per second of an Opus source that is copied into the container rather than encoded again. */
private const val MAX_COPY_BITS_PER_SECOND = 128_000

/** The input options that keep ffmpeg to local files of the audio formats taken in. */
private val inputGuards = listOf(
    "-protocol_whitelist", "file",
    "-format_whitelist", "mov,mp3,wav,flac,ogg,matroska,aac,aiff,caf",
)

/** Reads and encodes audio with the `ffmpeg` and `ffprobe` executables on the path, copying Opus that is already within spec. */
class FfmpegAudioClient(
    private val timeout: Duration = 5.minutes,
): AudioClient {

    override suspend fun readDuration(bytes: ByteArray): Duration? = withInputFile(bytes) { readDuration(it) }

    override suspend fun encode(bytes: ByteArray): ByteArray? = withInputFile(bytes) { input ->
        val encoded = Files.createTempFile("audio-out", ".m4a")
        try {
            runProcess(
                listOf("ffmpeg", "-v", "error", "-nostdin") + inputGuards + listOf(
                    "-i", input.toString(),
                    "-vn", "-map_metadata", "-1",
                ) + codecOptionsOf(input, bytes.size) + listOf(
                    "-movflags", "+faststart",
                    "-f", "mp4", "-y", encoded.toString(),
                )
            ) ?: return@withInputFile null
            encoded.readBytes().takeIf { it.isNotEmpty() }
        } finally {
            encoded.deleteIfExists()
        }
    }

    /**
     * The duration of the file at [input], from its header, or from the end of its last packet when the header has
     * none, as in a recording streamed by a browser.
     */
    private fun readDuration(input: Path): Duration? {
        val header = runProcess(
            listOf("ffprobe", "-v", "error") + inputGuards +
                listOf("-show_entries", "format=duration", "-of", "csv=p=0", input.toString())
        ) ?: return null
        header.trim().toDoubleOrNull()?.let { return it.seconds }

        val lastPacket = runProcess(
            listOf("ffprobe", "-v", "error") + inputGuards + listOf(
                "-select_streams", "a:0", "-show_entries", "packet=pts_time,duration_time",
                "-of", "csv=p=0", input.toString(),
            )
        )?.trim()?.lineSequence()?.lastOrNull() ?: return null
        return lastPacket.split(',').mapNotNull { it.toDoubleOrNull() }.sum().takeIf { it > 0 }?.seconds
    }

    /** The codec options for [input] of [size] bytes: a copy of Opus at or under [MAX_COPY_BITS_PER_SECOND], else an encode. */
    private fun codecOptionsOf(input: Path, size: Int): List<String> {
        val codec = runProcess(
            listOf("ffprobe", "-v", "error") + inputGuards + listOf(
                "-select_streams", "a:0", "-show_entries", "stream=codec_name", "-of", "csv=p=0", input.toString(),
            )
        )?.trim()
        val bitsPerSecond = readDuration(input)?.let { size * 8 / it.toDouble(DurationUnit.SECONDS) }
        return if (codec == "opus" && bitsPerSecond != null && bitsPerSecond <= MAX_COPY_BITS_PER_SECOND) {
            listOf("-c:a", "copy")
        } else {
            listOf("-c:a", "libopus", "-b:a", OPUS_BITRATE)
        }
    }

    private suspend fun <T> withInputFile(bytes: ByteArray, block: (Path) -> T): T = withContext(Dispatchers.IO) {
        val input = Files.createTempFile("audio-in", null)
        try {
            input.writeBytes(bytes)
            block(input)
        } finally {
            input.deleteIfExists()
        }
    }

    /** The output of [command], or `null` when it fails or runs past [timeout]. */
    private fun runProcess(command: List<String>): String? {
        val log = Files.createTempFile("audio-log", ".txt")
        try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start()
            if (!process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                logger.error { "${command.first()} timed out" }
                return null
            }
            val output = log.readText()
            if (process.exitValue() != 0) {
                logger.error { "${command.first()} failed: $output" }
                return null
            }
            return output
        } finally {
            log.deleteIfExists()
        }
    }
}
