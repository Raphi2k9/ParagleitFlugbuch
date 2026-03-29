plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    // iOS-Targets werden auf dem Mac mit Xcode ergänzt
    // listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    sourceSets {
        commonMain.dependencies {
            // Supabase
            implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.0")
            implementation("io.github.jan-tennert.supabase:auth-kt:3.0.0")

            // Ktor
            implementation("io.ktor:ktor-client-core:3.0.3")

            // Serialisierung
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        }

        androidMain.dependencies {
            implementation("io.ktor:ktor-client-android:3.0.3")
        }
    }
}

android {
    namespace = "com.example.flugbuch.shared"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
