plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.akshayasahayi.companion"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.akshayasahayi.companion"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Default Android & Compose Dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- AKSHAYA SAHAYI COMPANION DEPENDENCIES ---

    // Retrofit (API calls to Node.js)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp (API Logging)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Socket.IO (CRM Messenger Real-time connection)
    implementation("io.socket:socket.io-client:2.1.1") {
        exclude(group = "org.json", module = "json")
    }

    // Navigation (Fixes the AppNavigation.kt unresolved errors)
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // ViewModel (Fixes the SplashScreen.kt unresolved errors)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Lifecycle Runtime (Fixes LocalLifecycleOwner deprecation)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Preferences (DataStore for storing JWT and Device ID later)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil (For loading images smoothly)
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.compose.material:material-icons-extended")

    // --- CAMERA & QR SCANNER ---

    // CameraX (Hardware Camera Control)
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Google ML Kit (High-speed Barcode/QR Scanning)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

}