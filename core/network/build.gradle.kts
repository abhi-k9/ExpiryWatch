plugins {
    alias(libs.plugins.expirywatch.jvm.library)
    alias(libs.plugins.expirywatch.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
