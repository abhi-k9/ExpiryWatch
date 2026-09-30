package io.github.abhik9.expirywatch.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** SDK levels shared by every Android module. */
object AndroidSdk {
    const val COMPILE = 37
    const val TARGET = 37
    const val MIN = 26
}
