// Android library for P1 camera and MediaPipe pose input.
//
// This module is NOT included in settings.gradle.kts yet. Reason: this
// machine has no Android SDK, and adding it now would break the pure
// JVM movement build. After Android Studio with SDK is ready, add
// include(":camera") to settings.gradle.kts and sync.
//
// Versions used here:
// - CameraX 1.3.4 (camera-core, camera-camera2, camera-lifecycle, camera-view)
// - MediaPipe tasks-vision 0.10.26.1 (Pose Landmarker, LIVE_STREAM)
// - Model bundle: pose_landmarker_lite.task in app assets

plugins {
    id("com.android.library")
    kotlin("android")
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mediapipe:tasks-vision:0.10.26.1")
    implementation(project(":movement"))
}
