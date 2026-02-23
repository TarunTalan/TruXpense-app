import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

// Force consistent coroutines version to avoid layoutlib ServiceLoader conflicts
configurations.all {
    resolutionStrategy {
        // Match the play-services coroutines artifact used in the project
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3", "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    }
}

android {
    namespace = "com.example.truxpense"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.truxpense"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val webClientId: String? = (project.findProperty("googleWebClientId") as? String)
        if (!webClientId.isNullOrBlank()) {
            resValue("string", "default_web_client_id", webClientId)
        } else {
            resValue("string", "default_web_client_id", "REPLACE_WITH_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com")
        }

        // Backend base URL: prefer a project property `backendBaseUrl` (set in gradle.properties locally),
        // otherwise prefer an untracked local file 'gradle-local.properties' at project root, and finally
        // fall back to the development default.
        var backendBaseUrl: String? = (project.findProperty("backendBaseUrl") as? String)
        if (backendBaseUrl.isNullOrBlank()) {
            val localFile = rootProject.file("gradle-local.properties")
            if (localFile.exists()) {
                val props = Properties()
                localFile.inputStream().use { stream ->
                    props.load(stream)
                }
                backendBaseUrl = props.getProperty("backendBaseUrl")
            }
        }

        if (!backendBaseUrl.isNullOrBlank()) {
            // Ensure base URL ends with a trailing slash required by Retrofit
            if (!backendBaseUrl.endsWith("/")) backendBaseUrl += "/"
            resValue("string", "backend_base_url", backendBaseUrl)
        } else {
            resValue("string", "backend_base_url", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17  // Updated for Kotlin 2.1.0
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"  // Updated
    }

    buildFeatures {
        compose = true
    }

    // Exclude kotlinx.coroutines service provider from packaged resources to reduce layoutlib conflicts in preview
    packaging {
        resources {
            excludes += "META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory"
        }
    }

    // Remove composeOptions - handled by kotlin-compose plugin now
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.navigation.compose)

    implementation(libs.play.services.auth)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui)
    kapt(libs.hilt.compiler)

    // Room (local database)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)

    // Networking & JSON
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    // Use the versioned play-services coroutine artifact from the catalog
    implementation(libs.kotlinx.coroutines.play.services)

    // Secure storage
    implementation(libs.androidx.security.crypto)

    // Splashscreen
    implementation(libs.core.splashscreen)

    // DataStore
    implementation(libs.datastore.preferences)

    // Google Fonts
    implementation(libs.google.fonts.compose)

    // Hilt Navigation Compose
    implementation(libs.androidx.hilt.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}