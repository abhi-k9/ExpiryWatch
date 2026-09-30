plugins {
    alias(libs.plugins.expirywatch.android.library)
    alias(libs.plugins.expirywatch.android.room)
    alias(libs.plugins.expirywatch.hilt)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.database"
}

dependencies {
    api(projects.core.model)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
