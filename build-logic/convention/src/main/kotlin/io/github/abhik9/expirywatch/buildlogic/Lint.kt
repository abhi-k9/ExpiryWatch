package io.github.abhik9.expirywatch.buildlogic

import com.android.build.api.dsl.Lint

internal fun Lint.configureLint() {
    xmlReport = true
    sarifReport = true
    checkDependencies = true
    abortOnError = true
    // Dependency versions are managed centrally in the version catalog.
    disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
}
