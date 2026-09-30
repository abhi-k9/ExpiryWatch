plugins {
    alias(libs.plugins.expirywatch.android.library)
    alias(libs.plugins.expirywatch.hilt)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.scanner.mlkit"
}

dependencies {
    api(projects.core.scanner)
    implementation(libs.mlkit.barcodeScanning)
}
