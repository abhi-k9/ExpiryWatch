plugins {
    alias(libs.plugins.expirywatch.android.library.compose)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    implementation(libs.coil.compose)
}
