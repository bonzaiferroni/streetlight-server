package streetlight.server.service

import kampfire.model.ImageSize
import kampfire.model.Ok
import kotlinx.coroutines.test.runTest
import streetlight.server.external.isPublicHttpUrl
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImageIntakeTest {

    @Test
    fun `a small image is encoded`() {
        val outcome = encodeImage(png(64, 48), listOf(ImageSize.Thumb))
        assertIs<Ok<*>>(outcome, "a small png should encode")
    }

    @Test
    fun `an image declaring huge dimensions is refused before decoding`() {
        val bomb = png(8, 8).withPngSize(30_000, 30_000)
        assertEquals(ImageProblem.TooLarge, encodeImage(bomb, listOf(ImageSize.Thumb)), "a 30000 by 30000 header should be refused")
    }

    @Test
    fun `internal addresses are refused`() = runTest {
        listOf(
            "http://127.0.0.1/a.png",
            "http://localhost/a.png",
            "http://169.254.169.254/latest/meta-data/",
            "http://10.1.2.3/a.png",
            "http://192.168.0.1/a.png",
            "http://100.64.0.1/a.png",
            "http://[::1]/a.png",
            "http://[fd00::1]/a.png",
            "ftp://93.184.215.14/a.png",
            "file:///etc/passwd",
        ).forEach { assertFalse(isPublicHttpUrl(it), "$it should be refused") }
    }

    @Test
    fun `a public address is allowed`() = runTest {
        assertTrue(isPublicHttpUrl("https://93.184.215.14/a.png"), "a public ip literal should be allowed")
    }

    private fun png(width: Int, height: Int): ByteArray = ByteArrayOutputStream().also {
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", it)
    }.toByteArray()

    /** This png with its IHDR width and height rewritten, and the chunk's CRC fixed to match. */
    private fun ByteArray.withPngSize(width: Int, height: Int): ByteArray = copyOf().also { png ->
        fun putInt(at: Int, value: Int) = (0..3).forEach { png[at + it] = (value shr (24 - 8 * it)).toByte() }
        putInt(16, width)
        putInt(20, height)
        val crc = CRC32().apply { update(png, 12, 17) }.value.toInt()
        putInt(29, crc)
    }
}
