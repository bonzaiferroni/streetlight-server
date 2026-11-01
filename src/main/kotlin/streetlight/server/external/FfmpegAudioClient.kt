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

private val logger = KotlinLogging.logger {}

private const val OPUS_BITRATE = "96k"

/** The input options that keep ffmpeg to local files of the audio formats taken in. */
private val inputGuards = listOf(
    "-protocol_whitelist", "file",
    "-format_whitelist", "mov,mp3,wav,flac,ogg,matroska,aac,aiff,caf",
)

/** Reads and encodes audio with the `ffmpeg` and `ffprobe` executables on the path. */
class FfmpegAudioClient(
    private val timeout: Duration = 5.minutes,
): AudioClient {

    override suspend fun readDuration(bytes: ByteArray): Duration? = withInputFile(bytes) { input ->
        val output = runProcess(
            listOf("ffprobe", "-v", "error") + inputGuards +
                listOf("-show_entries", "format=duration", "-of", "csv=p=0", input.toString())
        ) ?: return@withInputFile null
        output.trim().toDoubleOrNull()?.seconds
    }

    override suspend fun encode(bytes: ByteArray): ByteArray? = withInputFile(bytes) { input ->
        val encoded = Files.createTempFile("audio-out", ".m4a")
        try {
            runProcess(
                listOf("ffmpeg", "-v", "error", "-nostdin") + inputGuards + listOf(
                    "-i", input.toString(),
                    "-vn", "-map_metadata", "-1",
                    "-c:a", "libopus", "-b:a", OPUS_BITRATE,
                    "-movflags", "+faststart",
                    "-f", "mp4", "-y", encoded.toString(),
                )
            ) ?: return@withInputFile null
            encoded.readBytes().takeIf { it.isNotEmpty() }
        } finally {
            encoded.deleteIfExists()
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
