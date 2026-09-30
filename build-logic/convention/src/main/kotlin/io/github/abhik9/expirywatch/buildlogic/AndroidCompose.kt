package io.github.abhik9.expirywatch.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Configures Jetpack Compose for an Android module. */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.apply {
        buildFeatures.apply {
            compose = true
        }
    }

    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        "implementation"(platform(bom))
        "testImplementation"(platform(bom))
        "androidTestImplementation"(platform(bom))
        "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // Treat our immutable domain models (and java.time types) as stable, so Compose can skip
        // recomposition of composables that receive them.
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("compose_compiler_config.conf"),
        )
    }
}
