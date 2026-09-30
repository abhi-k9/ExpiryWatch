plugins {
    alias(libs.plugins.expirywatch.jvm.library)
    alias(libs.plugins.expirywatch.hilt)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
}
