package io.github.abhik9.expirywatch.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/** Applies ktlint formatting checks (`./gradlew spotlessCheck` / `spotlessApply`) to a module. */
internal fun Project.configureSpotless() {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
        kotlin {
            target("src/**/*.kt")
            ktlint(ktlintVersion)
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(ktlintVersion)
            endWithNewline()
        }
    }
}
