plugins {
    alias(libs.plugins.expirywatch.jvm.library)
}

dependencies {
    api(projects.core.domain)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.kotlin.test)
    api(libs.turbine)
}
