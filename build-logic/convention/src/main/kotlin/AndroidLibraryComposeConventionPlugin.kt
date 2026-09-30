import com.android.build.api.dsl.LibraryExtension
import io.github.abhik9.expirywatch.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

abstract class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "expirywatch.android.library")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            configureAndroidCompose(extensions.getByType<LibraryExtension>())
        }
    }
}
