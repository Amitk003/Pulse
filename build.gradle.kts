// Plugin versions for all modules. Submodule build files apply
// these plugins WITHOUT a version number so focused branches can
// change one module without touching shared version logic.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    kotlin("android") version "2.0.20" apply false
    kotlin("jvm") version "2.0.20" apply false
    kotlin("plugin.compose") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}
