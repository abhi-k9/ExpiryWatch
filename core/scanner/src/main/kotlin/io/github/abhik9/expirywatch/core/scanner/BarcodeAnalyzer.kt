package io.github.abhik9.expirywatch.core.scanner

import androidx.camera.core.ImageAnalysis

/** Looks for barcodes in camera frames. Closed when the scanner goes away. */
interface BarcodeAnalyzer :
    ImageAnalysis.Analyzer,
    AutoCloseable

/**
 * Creates the analyzer for the scanner screen. Each distribution flavor binds its own: Google ML
 * Kit for the Play Store build, ZXing for the fully open-source build.
 */
fun interface BarcodeAnalyzerFactory {
    /** @param onBarcodeDetected called with the raw value of each barcode found, on any thread. */
    fun create(onBarcodeDetected: (String) -> Unit): BarcodeAnalyzer
}
