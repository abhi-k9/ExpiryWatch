package io.github.abhik9.expirywatch.buildlogic

import com.android.build.api.dsl.Lint
import java.io.File

internal fun Lint.configureLint() {
    xmlReport = true
    sarifReport = true
    // Print every issue to the console, so CI logs show more than the first failure.
    textReport = true
    textOutput = File("stdout")
    checkDependencies = true
    abortOnError = true
    // Dependency versions are managed centrally in the version catalog.
    disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
}
