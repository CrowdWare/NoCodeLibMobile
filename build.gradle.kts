import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDateTime
import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "at.crowdware.nocodelibmobile"
    compileSdk = 35

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"  //"1.5.0" // oder passend zu deinem Compose BOM
    }

    defaultConfig {
        minSdk = 29
        targetSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    
    // Compose-Abhängigkeiten
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(libs.androidx.material3)
    // Für Compose-Vorschau
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    // rest client
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    // grammar for parsing SML
    implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")

    // video player
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // youtube player
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // filament 3D
    implementation("com.google.android.filament:filament-android:1.54.5")
    implementation("com.google.android.filament:filament-utils-android:1.54.5")
    implementation("com.google.android.filament:gltfio-android:1.54.5")

    // async image
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

    // 👇 NEU ab Kotlin 2.0 – explizit hinzufügen!
    implementation("androidx.compose.compiler:compiler:1.5.10")
}
