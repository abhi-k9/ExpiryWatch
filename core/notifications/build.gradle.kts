plugins {
    alias(libs.plugins.expirywatch.android.library)
    alias(libs.plugins.expirywatch.hilt)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.notifications"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.ktx)
    ksp(libs.androidx.hilt.compiler)
}
