plugins {
    alias(libs.plugins.expirywatch.android.library.compose)
    alias(libs.plugins.expirywatch.hilt)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.scanner"
}

dependencies {
    api(libs.androidx.camera.core)
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.hilt.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
}
