plugins {
    alias(libs.plugins.expirywatch.android.feature)
}

android {
    namespace = "io.github.abhik9.expirywatch.feature.editor"
}

dependencies {
    implementation(projects.core.scanner)
    implementation(libs.coil.compose)
}
