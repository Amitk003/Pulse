pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pulse"
include(":movement")
include(":camera")
include(":app")
include(":data")
include(":bridge")
