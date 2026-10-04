package io.github.abhik9.expirywatch.core.scanner.zxing

import androidx.camera.core.ImageProxy
import io.github.abhik9.expirywatch.core.scanner.BarcodeAnalyzer

/** Reads barcodes with ZXing, a fully open-source library with no Google Play dependency. */
internal class ZxingBarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit,
) : BarcodeAnalyzer {
    private val decoder = LuminanceBarcodeDecoder()
    private var buffer = ByteArray(0)

    override fun analyze(image: ImageProxy) {
        image.use {
            // Plane 0 of a YUV_420_888 frame is the luminance (grayscale) channel.
            val plane = image.planes[0]
            val data = plane.buffer
            data.rewind()
            if (buffer.size != data.remaining()) buffer = ByteArray(data.remaining())
            data.get(buffer)

            val barcode = try {
                decoder.decode(
                    luminance = buffer,
                    rowStride = plane.rowStride,
                    width = image.width,
                    height = image.height,
                    rotationDegrees = image.imageInfo.rotationDegrees,
                )
            } catch (e: RuntimeException) {
                // A frame ZXing can't handle; an exception here would end the analysis thread.
                null
            }
            barcode?.let(onBarcodeDetected)
        }
    }

    override fun close() = Unit
}
