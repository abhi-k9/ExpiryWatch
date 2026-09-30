import java.util.Properties

plugins {
    alias(libs.plugins.expirywatch.android.application)
    alias(libs.plugins.expirywatch.android.application.compose)
    alias(libs.plugins.expirywatch.hilt)
}

// Release signing reads keystore.properties (not checked in) when present; see README.
val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "io.github.abhik9.expirywatch"

    defaultConfig {
        applicationId = "io.github.abhik9.expirywatch"
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "SOURCE_CODE_URL", "\"https://github.com/abhi-k9/expirywatch\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // "play" uses Google ML Kit for barcode scanning; "foss" uses ZXing and contains no
    // proprietary Google libraries, so it can be distributed on F-Droid.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("String", "BARCODE_ENGINE", "\"Google ML Kit\"")
        }
        create("foss") {
            dimension = "distribution"
            buildConfigField("String", "BARCODE_ENGINE", "\"ZXing\"")
        }
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without a release keystore, sign with the debug key so release builds still install.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.navigation)
    implementation(projects.core.network)
    implementation(projects.core.notifications)
    implementation(projects.core.ui)
    implementation(projects.feature.editor)
    implementation(projects.feature.insights)
    implementation(projects.feature.items)
    implementation(projects.feature.settings)
    implementation(projects.feature.widget)

    "playImplementation"(projects.core.scannerMlkit)
    "fossImplementation"(projects.core.scannerZxing)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.navigationSuite)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.work.ktx)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
