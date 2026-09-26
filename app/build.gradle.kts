plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.livefree"
    compileSdk = 35

    // Live Free's signing key comes from environment variables (GitHub secrets in the
    // release workflow), never from the repository. Without them, release builds are
    // unsigned and debug builds use this machine's debug key.
    val keystoreFile = System.getenv("LIVEFREE_KEYSTORE_FILE")?.let(::file)?.takeIf { it.exists() }
    val liveFreeSigning = keystoreFile?.let {
        signingConfigs.create("liveFree") {
            storeFile = it
            storePassword = System.getenv("LIVEFREE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("LIVEFREE_KEY_ALIAS")
            keyPassword = System.getenv("LIVEFREE_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.livefree"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.6.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = liveFreeSigning
        }
        debug {
            // Same key as releases when available, so either can update the other.
            liveFreeSigning?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // The bundled ML Kit scanner ships native code for every CPU type. Per-CPU APKs
    // keep downloads small; the universal APK still works on any phone.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:security"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:nfc"))
    implementation(project(":feature:qr"))
    implementation(project(":feature:service"))
    implementation(project(":feature:blocker"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
