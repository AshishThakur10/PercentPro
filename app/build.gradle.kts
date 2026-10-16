import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

// storeFile is resolved relative to the project root, not the app/ module, so
// the same value works regardless of where this file is evaluated from.
// See keystore.properties.example.
val releaseStoreFile = keystoreProperties.getProperty("storeFile")
    ?.takeIf { it.isNotBlank() }
    ?.let { rootProject.file(it) }
val hasReleaseKeystore = keystorePropertiesFile.exists() && releaseStoreFile?.exists() == true

android {
    namespace = "com.atlabs.percentpro"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.atlabs.percentpro"
        minSdk = 23
        targetSdk = 36
        // versionCode is the integer Play orders releases by and must only ever
        // increase; versionName is what a reader sees. Settings reads this at
        // runtime from PackageManager, so there is no second copy to keep in step.
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only register a usable config when the keystore is actually present.
        // A config pointing at a missing file fails with a much worse message.
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Enabled in Phase 9. Safe for this app specifically, not as a
            // default: the app has no reflection, no serialization library, no
            // WebView and no dependency-injection framework, so nothing resolves
            // classes or members by name at runtime. Compose, DataStore and the
            // rest of AndroidX all ship their own consumer rules.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            // Stated explicitly rather than inherited. The release block above
            // must never be able to turn debuggability on, and an upload of a
            // debuggable build is the failure this prevents.
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

    // The app switches language in-app, from its own settings screen, with no
    // download step. App Bundles split languages out of the base APK by
    // default, which would leave the in-app switcher working only if Play had
    // already installed the other language. English and Hindi together are a
    // negligible part of the APK, so both stay in the base and the switcher
    // always works, offline, on first run.
    bundle {
        language {
            enableSplit = false
        }
    }
}

// An unsigned release APK still "builds successfully" but fails at install time
// with INSTALL_PARSE_FAILED_NO_CERTIFICATES, which hides the real cause. Refuse
// to produce one. Debug builds are unaffected, so a fresh clone without a
// keystore can still compile and test.
if (!hasReleaseKeystore) {
    tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
        .configureEach {
            doFirst {
                throw GradleException(
                    "Release signing is not configured. Copy keystore.properties.example " +
                        "to keystore.properties at the project root and point storeFile at " +
                        "your keystore. Refusing to build an unsigned release APK."
                )
            }
        }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}