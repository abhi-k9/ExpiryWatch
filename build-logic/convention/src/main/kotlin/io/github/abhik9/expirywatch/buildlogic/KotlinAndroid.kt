package io.github.abhik9.expirywatch.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Configures the base Kotlin and Android options shared by application and library modules. */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = AndroidSdk.COMPILE

        defaultConfig.apply {
            minSdk = AndroidSdk.MIN
        }

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    configureKotlin<KotlinAndroidProjectExtension>()
}

/** Configures Kotlin options for pure JVM (non-Android) modules. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configureKotlin<KotlinJvmProjectExtension>()
}

private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // Treat all Kotlin warnings as errors when -PwarningsAsErrors=true is passed.
    val warningsAsErrors = providers.gradleProperty("warningsAsErrors")
        .map(String::toBoolean)
        .orElse(false)
    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> error("Unsupported project extension $this ${T::class}")
    }.apply {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = warningsAsErrors
    }
}
