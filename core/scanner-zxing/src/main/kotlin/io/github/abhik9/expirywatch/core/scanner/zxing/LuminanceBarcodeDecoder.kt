package io.github.abhik9.expirywatch.core.scanner.zxing

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

/**
 * Decodes barcodes from a grayscale (luminance) image, such as the Y plane of a camera frame.
 * Free of Android dependencies, so it can be unit tested on the JVM.
 */
class LuminanceBarcodeDecoder {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to SUPPORTED_FORMATS,
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }

    /**
     * @param luminance one byte per pixel, row by row.
     * @param rowStride bytes per row in [luminance], which can exceed [width] because of padding.
     * @param rotationDegrees clockwise rotation that makes the image upright (0, 90, 180 or 270).
     * @return the barcode's text, or `null` if no barcode was found.
     */
    fun decode(luminance: ByteArray, rowStride: Int, width: Int, height: Int, rotationDegrees: Int): String? {
        val upright = rotateClockwise(compact(luminance, rowStride, width, height), width, height, rotationDegrees)
        val source = PlanarYUVLuminanceSource(
            upright.pixels,
            upright.width,
            upright.height,
            0,
            0,
            upright.width,
            upright.height,
            false,
        )
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (e: ReaderException) {
            // No (readable) barcode in this frame; the next one may do better.
            null
        } finally {
            reader.reset()
        }
    }

    internal class Image(val pixels: ByteArray, val width: Int, val height: Int)

    internal companion object {
        val SUPPORTED_FORMATS = listOf(
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.ITF,
            BarcodeFormat.QR_CODE,
            BarcodeFormat.DATA_MATRIX,
        )

        /** Drops any row padding, so rows are exactly [width] bytes long. */
        fun compact(data: ByteArray, rowStride: Int, width: Int, height: Int): ByteArray {
            if (rowStride == width) return data.copyOf(width * height)
            val result = ByteArray(width * height)
            for (row in 0 until height) {
                System.arraycopy(data, row * rowStride, result, row * width, width)
            }
            return result
        }

        fun rotateClockwise(pixels: ByteArray, width: Int, height: Int, degrees: Int): Image {
            val rotated = ByteArray(pixels.size)
            return when (Math.floorMod(degrees, FULL_TURN)) {
                0 -> Image(pixels, width, height)

                QUARTER_TURN -> {
                    // (x, y) -> (height - 1 - y, x) in an image `height` wide.
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            rotated[x * height + (height - 1 - y)] = pixels[y * width + x]
                        }
                    }
                    Image(rotated, height, width)
                }

                HALF_TURN -> {
                    for (i in pixels.indices) rotated[pixels.size - 1 - i] = pixels[i]
                    Image(rotated, width, height)
                }

                THREE_QUARTER_TURN -> {
                    // (x, y) -> (y, width - 1 - x) in an image `height` wide.
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            rotated[(width - 1 - x) * height + y] = pixels[y * width + x]
                        }
                    }
                    Image(rotated, height, width)
                }

                else -> error("Unsupported rotation: $degrees")
            }
        }

        private const val QUARTER_TURN = 90
        private const val HALF_TURN = 180
        private const val THREE_QUARTER_TURN = 270
        private const val FULL_TURN = 360
    }
}
