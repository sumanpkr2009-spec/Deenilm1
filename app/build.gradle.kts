plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.deenilm.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.deenilm.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Debug keystore is stored base64-encoded (debug.keystore.b64) because the GitHub
    // API push used here cannot transfer binary files. Decode it at configuration time
    // so every CI build signs with the SAME key (reinstalls keep working).
    val decodedKeystore = File(buildDir, "generated/debug.keystore").apply {
        parentFile.mkdirs()
        if (!exists()) {
            val b64 = file("debug.keystore.b64").readText().trim()
            writeBytes(java.util.Base64.getDecoder().decode(b64))
        }
    }

    // Launcher icons are stored base64-encoded (src/main/res/icon-b64/*.txt) because
    // the GitHub API push used here cannot transfer binary files. Decode them at
    // configuration time so the manifest's @mipmap/ic_launcher references resolve.
    val iconDensities = mapOf(
        "mdpi" to "mdpi.txt",
        "hdpi" to "hdpi.txt",
        "xhdpi" to "xhdpi.txt",
        "xxhdpi" to "xxhdpi.txt",
        "xxxhdpi" to "xxxhdpi.txt"
    )
    iconDensities.forEach { (density, b64file) ->
        val pngBytes = java.util.Base64.getDecoder().decode(
            file("src/main/res/icon-b64/$b64file").readText().trim()
        )
        val outDir = file("src/main/res/mipmap-$density").apply { mkdirs() }
        listOf("ic_launcher.png", "ic_launcher_round.png").forEach { name ->
            val out = File(outDir, name)
            if (!out.exists()) out.writeBytes(pngBytes)
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = decodedKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.batoulapps.adhan:adhan2:0.0.7")
}
