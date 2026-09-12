// Android library for P1 camera and MediaPipe pose input.
//
// This module is NOT included in settings.gradle.kts yet. Reason: this
// machine has no Android SDK, and adding it now would break the pure
// JVM movement build. After Android Studio with SDK is ready, add
// include(":camera") to settings.gradle.kts and sync.
//
// Versions used here:
// - CameraX 1.3.4 (camera-core, camera-camera2, camera-lifecycle, camera-view)
// - MediaPipe tasks-vision latest.release (Pose Landmarker, LIVE_STREAM)
// - Model bundle: pose_landmarker_lite.task in app assets

plugins {
    id("com.android.library") version "8.5.2"
    kotlin("android") version "2.0.20"
}

android {
    namespace = "com.pulse.camera"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mediapipe:tasks-vision:latest.release")
    implementation(project(":movement"))

    // Jetpack Compose Kotlin UI
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
}
