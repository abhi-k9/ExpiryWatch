import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "io.github.abhik9.expirywatch.buildlogic"

// The convention plugins run inside Gradle, so they target the JDK used to build the project,
// which is unrelated to what runs on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradleApiPlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.expirywatch.android.application.asProvider().get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = libs.plugins.expirywatch.android.application.compose.get().pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.expirywatch.android.library.asProvider().get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = libs.plugins.expirywatch.android.library.compose.get().pluginId
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.expirywatch.android.feature.get().pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidRoom") {
            id = libs.plugins.expirywatch.android.room.get().pluginId
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.expirywatch.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.expirywatch.jvm.library.get().pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
