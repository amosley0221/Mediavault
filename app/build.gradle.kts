import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing: CI (or a local build) can point at a private keystore through env vars.
// If none is provided we fall back to the checked-in project keystore so that every
// build produces an APK that installs — and upgrades — on a phone.
val keystoreFile = (System.getenv("MV_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }?.let { file(it) })
    ?: rootProject.file("keystore/mediavault.jks")
val keystorePassword = System.getenv("MV_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "mediavault"
val keyAliasName = System.getenv("MV_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "mediavault"
val keyPassword = System.getenv("MV_KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: keystorePassword

android {
    namespace = "com.mediavault.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mediavault.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("MV_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("MV_VERSION_NAME") ?: "1.0.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("mediavault") {
            storeFile = keystoreFile
            storePassword = keystorePassword
            keyAlias = keyAliasName
            this.keyPassword = keyPassword
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystoreFile.exists()) signingConfigs.getByName("mediavault") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
