plugins {
    alias(libs.plugins.expirywatch.android.library.compose)
    alias(libs.plugins.expirywatch.hilt)
}

android {
    namespace = "io.github.abhik9.expirywatch.feature.widget"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}
