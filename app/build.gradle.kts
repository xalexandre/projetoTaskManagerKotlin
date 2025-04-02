plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.firebase.crashlytics)
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "dev.brodt.taskmanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.brodt.taskmanager"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Enable Crashlytics in debug builds for testing
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            // Enable Crashlytics NDK reports
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Always enable Crashlytics in release builds
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            // Enable Crashlytics NDK reports
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.circleimageview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")
    
    // Firebase Analytics
    implementation(libs.firebase.analytics)
    
    // Firebase Crashlytics
    implementation(libs.firebase.crashlytics)
    
    // Firebase Cloud Messaging for push notifications
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    implementation("com.google.firebase:firebase-functions:20.4.0")
    
    // Firebase App Check (required by Firebase Database and Storage)
    implementation("com.google.firebase:firebase-appcheck:17.1.1")
    implementation("com.google.firebase:firebase-appcheck-playintegrity:17.1.1")
    implementation("com.google.firebase:firebase-appcheck-interop:17.1.0")
    implementation("com.google.firebase:firebase-database-ktx:20.3.0")
    
    // Firebase Performance Monitoring
    implementation("com.google.firebase:firebase-perf-ktx:20.5.1")
    
    // AdMob
    implementation("com.google.android.gms:play-services-ads:21.5.0")
}

// Handle manifest merger conflicts
android {
    // ... existing config ...
    
    configurations.all {
        resolutionStrategy {
            force("com.google.android.gms:play-services-measurement-api:21.5.1")
            force("com.google.android.gms:play-services-measurement-sdk-api:21.5.1")
            force("com.google.android.gms:play-services-measurement-base:21.5.1")
            force("com.google.android.gms:play-services-measurement-impl:21.5.1")
        }
    }
}
