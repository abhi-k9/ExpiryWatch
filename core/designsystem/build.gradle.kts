plugins {
    alias(libs.plugins.expirywatch.android.library.compose)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.designsystem"
}

dependencies {
    // Exported so modules that use Compose only through this one (e.g. for shared strings) still
    // resolve the version-less Compose artifacts.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material.iconsExtended)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
}
