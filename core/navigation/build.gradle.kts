plugins {
    alias(libs.plugins.expirywatch.android.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(projects.core.common)
    api(projects.core.model)
    implementation(libs.kotlinx.serialization.json)
}
