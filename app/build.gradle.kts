import java.io.FileInputStream
import java.util.Properties

// ---------------------------------------------------------------------------
// google-services.json is produced by a human from the Firebase console
// (See PRODUCTION_READINESS.md) and is not present in a fresh checkout.
// Applying com.google.gms.google-services without that file hard-fails the
// build for every other agent/developer today, so we gate it on the file's
// existence and surface a loud, unmissable warning instead of failing.
// ---------------------------------------------------------------------------
val googleServicesFile = file("google-services.json")
val hasGoogleServicesFile = googleServicesFile.exists()

// ---------------------------------------------------------------------------
// keystore.properties is git-ignored and only exists once a real release
// keystore has been generated. The build must remain green without it -
// release builds silently fall back to debug signing in that case, but we
// warn loudly so nobody accidentally ships a debug-signed release.
// ---------------------------------------------------------------------------
val keystorePropertiesFile = file("keystore.properties")
val hasKeystoreProperties = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasKeystoreProperties) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

// Production backend deployed on Render (free tier)
// staging/release read from a Gradle property (-PSTAGING_API_BASE_URL=...,
// -PRELEASE_API_BASE_URL=...) with production URL as default
val stagingApiBaseUrl = (project.findProperty("STAGING_API_BASE_URL") as String?)
    ?: "https://glowupai-20ca.onrender.com/api/"
val releaseApiBaseUrl = (project.findProperty("RELEASE_API_BASE_URL") as String?)
    ?: "https://glowupai-20ca.onrender.com/api/"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    // com.google.gms.google-services and com.google.firebase.crashlytics are
    // applied conditionally below, once google-services.json is confirmed to
    // exist - see the block after `android { ... }`.
}

android {
    namespace = "com.glowup.ai"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.glowup.ai"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasKeystoreProperties) {
                storeFile = file(keystoreProperties.getProperty("storeFile", "release.keystore"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Temporarily commented out so debug build uses com.glowup.ai (matches Firebase)
            // TODO: Add com.glowup.ai.debug to Firebase and uncomment this
            // applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("String", "API_BASE_URL", "\"https://glowupai-20ca.onrender.com/api/\"")
        }

        create("staging") {
            initWith(getByName("debug"))
            // Temporarily commented out so staging uses com.glowup.ai (matches Firebase)
            // TODO: Add com.glowup.ai.staging to Firebase and uncomment this
            // applicationIdSuffix = ".staging"
            isDebuggable = false
            matchingFallbacks += listOf("debug")
            buildConfigField("String", "API_BASE_URL", "\"$stagingApiBaseUrl\"")
            signingConfig = if (hasKeystoreProperties) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            signingConfig = if (hasKeystoreProperties) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    // NOTE: jvmToolchain(17) previously forced Gradle to provision a JDK 17
    // toolchain. This machine only has JDK 25 installed, and toolchain
    // auto-download is blocked by the network gateway (services.gradle.org
    // / githubusercontent asset downloads return 403). Using jvmToolchain(25)
    // uses the already-installed JDK directly; compileOptions/jvmTarget below
    // still target JVM 17 bytecode, which javac/kotlinc on JDK 25 can do
    // without needing a JDK 17 install.
    jvmToolchain(25)
}

if (hasGoogleServicesFile) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
} else {
    val warning = buildString {
        appendLine()
        appendLine("!".repeat(78))
        appendLine("WARNING: app/google-services.json is missing.")
        appendLine("The com.google.gms.google-services and com.google.firebase.crashlytics")
        appendLine("Gradle plugins were NOT applied. Firebase Auth/Analytics/Crashlytics")
        appendLine("dependencies will still compile, but Firebase will not be configured and")
        appendLine("will fail to initialize at runtime.")
        appendLine("Fix: create the 'glowup-ai' Firebase project (see PRODUCTION_READINESS.md),")
        appendLine("download google-services.json from the console, and place it at")
        appendLine("app/google-services.json, then re-sync Gradle.")
        appendLine("!".repeat(78))
    }
    logger.warn(warning)
}

if (!hasKeystoreProperties) {
    logger.warn(
        "WARNING: app/keystore.properties not found (see app/keystore.properties.example). " +
            "Release build type will fall back to debug signing - DO NOT distribute a release " +
            "build signed this way."
    )
}

dependencies {
    // Core library desugaring for java.time APIs on API < 26
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation (type-safe routes) + kotlinx.serialization for route args
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Networking: Retrofit + OkHttp + kotlinx.serialization (no Gson)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    // Persistence
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)

    // CameraX + ML Kit face detection
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.face.detection)

    // Coil (Compose image loading)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
