import com.android.build.api.dsl.ApplicationExtension
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

abstract class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = AndroidSdk.TARGET
                testOptions.animationsDisabled = true
                testOptions.unitTests.isIncludeAndroidResources = true
                lint { configureLint() }
            }
            configureSpotless()

            dependencies {
                "testImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("junit").get())
            }
        }
    }
}
