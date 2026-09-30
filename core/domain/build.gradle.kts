plugins {
    alias(libs.plugins.expirywatch.jvm.library)
    alias(libs.plugins.expirywatch.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.common)
    api(projects.core.model)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
