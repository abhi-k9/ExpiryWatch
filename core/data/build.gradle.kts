plugins {
    alias(libs.plugins.expirywatch.android.library)
    alias(libs.plugins.expirywatch.hilt)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(libs.androidx.room.ktx)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
