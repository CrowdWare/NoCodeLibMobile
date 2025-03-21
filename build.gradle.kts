plugins {
    id("com.android.library") version "8.0.0" // Beispielversion, bitte anpassen
    kotlin("android") version "1.8.0" // Beispielversion, bitte anpassen
}

android {
    namespace = "at.crowdware.nocodelibmobile"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33       
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation("androidx.compose.ui:ui:1.3.0")
    implementation("androidx.compose.material:material:1.3.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0")
}

repositories {
    google()
    mavenCentral()
}