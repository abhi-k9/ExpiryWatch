package io.github.abhik9.expirywatch.core.common

/**
 * Facts about the running build, provided by the app module.
 *
 * @property barcodeEngine the name of the barcode scanning library in this build's flavor.
 */
data class AppInfo(
    val versionName: String,
    val barcodeEngine: String,
    val sourceCodeUrl: String,
)
