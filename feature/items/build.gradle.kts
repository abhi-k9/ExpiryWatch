plugins {
    alias(libs.plugins.expirywatch.android.feature)
}

android {
    namespace = "io.github.abhik9.expirywatch.feature.items"
}

dependencies {
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    // Registers the empty activity the Compose test rule shows content in.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
