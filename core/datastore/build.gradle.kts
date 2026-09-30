plugins {
    alias(libs.plugins.expirywatch.android.library)
    alias(libs.plugins.expirywatch.hilt)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.datastore"
}

dependencies {
    api(libs.androidx.dataStore.preferences)
    api(projects.core.model)
    implementation(projects.core.common)

    testImplementation(libs.kotlinx.coroutines.test)
}
