plugins {
    alias(libs.plugins.expirywatch.android.library)
    alias(libs.plugins.expirywatch.hilt)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.scanner.zxing"
}

dependencies {
    api(projects.core.scanner)
    implementation(libs.zxing.core)
}
