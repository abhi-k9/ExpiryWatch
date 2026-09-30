import io.github.abhik9.expirywatch.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module: a Compose UI library with Hilt, wired to the shared design system, UI
 * components, navigation keys and domain layer.
 */
abstract class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "expirywatch.android.library.compose")
            apply(plugin = "expirywatch.hilt")

            dependencies {
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:model"))
                "implementation"(project(":core:navigation"))
                "implementation"(project(":core:ui"))

                "implementation"(libs.findLibrary("androidx.activity.compose").get())
                "implementation"(libs.findLibrary("androidx.compose.material.iconsExtended").get())
                "implementation"(libs.findLibrary("androidx.compose.material3").get())
                "implementation"(libs.findLibrary("androidx.hilt.lifecycle.viewModelCompose").get())
                "implementation"(libs.findLibrary("androidx.lifecycle.runtimeCompose").get())
                "implementation"(libs.findLibrary("androidx.lifecycle.viewModelCompose").get())
                "implementation"(libs.findLibrary("androidx.navigation3.runtime").get())

                "testImplementation"(project(":core:testing"))
            }
        }
    }
}
