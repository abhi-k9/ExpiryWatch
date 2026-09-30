import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import io.github.abhik9.expirywatch.buildlogic.AndroidSdk
import io.github.abhik9.expirywatch.buildlogic.configureKotlinAndroid
import io.github.abhik9.expirywatch.buildlogic.configureLint
import io.github.abhik9.expirywatch.buildlogic.configureSpotless
import io.github.abhik9.expirywatch.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = AndroidSdk.TARGET
                testOptions.animationsDisabled = true
                testOptions.unitTests.isIncludeAndroidResources = true
                lint.targetSdk = AndroidSdk.TARGET
                lint { configureLint() }
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                // Only create instrumented test variants for modules that actually have them.
                beforeVariants {
                    it.androidTest.enable = it.androidTest.enable &&
                        projectDir.resolve("src/androidTest").exists()
                }
            }
            configureSpotless()

            dependencies {
                "testImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("junit").get())
            }
        }
    }
}
