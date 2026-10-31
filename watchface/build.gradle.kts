plugins {
    alias(libs.plugins.android.application)
}

android {
    base.archivesName.set("Salarycalculator-watchface")
    enableKotlin = false
    namespace = "com.example.salarycalculator.watchface"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.salarycalculator.watchface"
        minSdk = 33
        targetSdk = 36
        versionCode = 72
        versionName = "27.8"
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
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

tasks.matching {
    it.name.contains("Dex", ignoreCase = true) ||
    it.name.contains("Desugar", ignoreCase = true) ||
    it.name.contains("ArtProfile", ignoreCase = true)
}.configureEach {
    enabled = false
}

val wfBuildDirPath = layout.buildDirectory.asFile.get().absolutePath
tasks.matching { it.name == "optimizeReleaseResources" }.configureEach {
    notCompatibleWithConfigurationCache("Preserves unoptimized WFF resource paths for Wear OS 5+")
    doLast {
        val unoptimized = File(
            wfBuildDirPath,
            "intermediates/linked_resources_binary_format/release/processReleaseResources/linked-resources-binary-format-release.ap_"
        )
        val optimized = File(
            wfBuildDirPath,
            "intermediates/optimized_processed_res/release/optimizeReleaseResources/resources-release-optimize.ap_"
        )
        if (unoptimized.exists()) {
            unoptimized.copyTo(optimized, overwrite = true)
        }
    }
}
