import io.github.abhik9.expirywatch.buildlogic.configureKotlinJvm
import io.github.abhik9.expirywatch.buildlogic.configureSpotless
import io.github.abhik9.expirywatch.buildlogic.configureTests
import io.github.abhik9.expirywatch.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** A pure Kotlin/JVM module with no Android dependencies, e.g. the domain layer. */
abstract class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")

            configureKotlinJvm()
            configureSpotless()
            configureTests()

            dependencies {
                "testImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("junit").get())
            }
        }
    }
}
