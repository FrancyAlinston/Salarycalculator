plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    base.archivesName.set("Salarycalculator-wear")
    namespace = "com.example.salarycalculator.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.salarycalculator.wear"
        minSdk = 28
        targetSdk = 36
        versionCode = 68
        versionName = "27.4"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../app/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = false
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Wear Compose
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.wear)

    // Play Services Wearable (Data Layer Sync)
    implementation(libs.play.services.wearable)

    // DataStore & Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    // Compose UI & Tooling
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
