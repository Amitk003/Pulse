import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

group = "com.pulse"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // JSON wire format for the bridge contracts (PlayerSnapshot / FightResult).
    implementation("org.json:json:20231013")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnit()
}
