package io.github.abhik9.expirywatch.core.scanner.zxing

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class LuminanceBarcodeDecoderTest {
    private val decoder = LuminanceBarcodeDecoder()

    private fun BitMatrix.toLuminance(): ByteArray = ByteArray(width * height) { i ->
        if (get(i % width, i / width)) 0 else 255.toByte()
    }

    private fun render(text: String, format: BarcodeFormat, width: Int, height: Int): LuminanceBarcodeDecoder.Image {
        val matrix = MultiFormatWriter().encode(text, format, width, height)
        return LuminanceBarcodeDecoder.Image(matrix.toLuminance(), matrix.width, matrix.height)
    }

    @Test
    fun decodesAnUprightEan13() {
        val image = render("4006381333931", BarcodeFormat.EAN_13, 400, 160)

        assertEquals(
            "4006381333931",
            decoder.decode(image.pixels, image.width, image.width, image.height, rotationDegrees = 0),
        )
    }

    @Test
    fun decodesAFrameThatNeedsRotating() {
        // A phone held upright sees a horizontal barcode rotated 90° in its landscape sensor frame.
        val upright = render("4006381333931", BarcodeFormat.EAN_13, 400, 160)
        val sensorFrame = LuminanceBarcodeDecoder.rotateClockwise(upright.pixels, upright.width, upright.height, 270)

        assertEquals(
            "4006381333931",
            decoder.decode(sensorFrame.pixels, sensorFrame.width, sensorFrame.width, sensorFrame.height, 90),
        )
    }

    @Test
    fun ignoresRowPadding() {
        val image = render("https://example.com", BarcodeFormat.QR_CODE, 200, 200)
        val stride = image.width + 16
        val padded = ByteArray(stride * image.height)
        for (row in 0 until image.height) {
            System.arraycopy(image.pixels, row * image.width, padded, row * stride, image.width)
        }

        assertEquals("https://example.com", decoder.decode(padded, stride, image.width, image.height, 0))
    }

    @Test
    fun returnsNullWithoutABarcode() {
        val blank = ByteArray(320 * 240) { 255.toByte() }
        assertNull(decoder.decode(blank, 320, 320, 240, 0))
    }

    @Test
    fun rotationsRoundTrip() {
        val pixels = ByteArray(6) { it.toByte() } // 3 x 2
        var image = LuminanceBarcodeDecoder.Image(pixels, 3, 2)
        repeat(4) { image = LuminanceBarcodeDecoder.rotateClockwise(image.pixels, image.width, image.height, 90) }

        assertEquals(pixels.toList(), image.pixels.toList())
        assertEquals(3, image.width)
        assertEquals(
            listOf<Byte>(3, 0, 4, 1, 5, 2),
            LuminanceBarcodeDecoder.rotateClockwise(pixels, 3, 2, 90).pixels.toList(),
        )
    }
}
