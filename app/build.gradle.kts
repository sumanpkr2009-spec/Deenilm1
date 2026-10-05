plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Decode binary assets (keystore + icons) at the TOP LEVEL, outside android {},
// where Project.file() and buildDir resolve unambiguously.
// TEMP DISABLED: keystore decode
val decodedKeystore: java.io.File = project.file("debug.keystore.b64")
mapOf(
    "mdpi" to "mdpi.txt",
    "hdpi" to "hdpi.txt",
    "xhdpi" to "xhdpi.txt",
    "xxhdpi" to "xxhdpi.txt",
    "xxxhdpi" to "xxxhdpi.txt"
).forEach { (density, b64file) ->
    val pngBytes = java.util.Base64.getDecoder().decode(
        project.file("src/main/res/icon-b64/$b64file").readText().trim()
    )
    val outDir = project.file("src/main/res/mipmap-$density").apply { mkdirs() }
    listOf("ic_launcher.png", "ic_launcher_round.png").forEach { name ->
        val out = java.io.File(outDir, name)
        if (!out.exists()) out.writeBytes(pngBytes)
    }
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
    implementation("com.batoulapps.adhan:adhan2-jvm:0.0.7")
}
