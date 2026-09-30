package io.github.abhik9.expirywatch.core.scanner.mlkit

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.abhik9.expirywatch.core.scanner.BarcodeAnalyzer

/** Reads barcodes with Google ML Kit's bundled, on-device model. */
internal class MlKitBarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit,
) : BarcodeAnalyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
            )
            .build(),
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        scanner.process(InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { barcode -> barcode.rawValue?.takeIf { it.isNotBlank() } }
                    ?.let(onBarcodeDetected)
            }
            // The frame must stay open until ML Kit is done with it.
            .addOnCompleteListener { image.close() }
    }

    override fun close() = scanner.close()
}
