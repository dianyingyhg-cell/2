plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.local.douyinmaker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.local.douyinmaker"
        minSdk = 29
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = false
    }
}

dependencies {
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-transformer:1.11.1")
}
