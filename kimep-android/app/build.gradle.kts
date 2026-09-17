import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing credentials live in keystore.properties (git-ignored).
// If the file is absent (e.g. a fresh clone), release builds fall back to the
// debug signing config so `assembleRelease` still works.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

// Analytics config is opt-in and never committed: set umami.host / umami.websiteId in
// local.properties, or pass -Pumami.host=… / UMAMI_HOST env vars (CI uses secrets).
// When empty, the app ships with analytics fully disabled (no network calls at all).
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun configValue(key: String, env: String): String =
    localProperties.getProperty(key)
        ?: (project.findProperty(key) as String?)
        ?: System.getenv(env)
        ?: ""

android {
    namespace = "kz.kimep.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "kz.kimep.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2-beta"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "UMAMI_HOST", "\"${configValue("umami.host", "UMAMI_HOST")}\"")
        buildConfigField(
            "String",
            "UMAMI_WEBSITE_ID",
            "\"${configValue("umami.websiteId", "UMAMI_WEBSITE_ID")}\"",
        )
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (keystorePropsFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    debugImplementation(libs.androidx.ui.tooling)
}
