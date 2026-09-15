import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile = file("keystore.properties")
val hasKeystoreProperties = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasKeystoreProperties) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

// Android projects conventionally keep machine-local, non-versioned settings here. The debug
// build documentation already directs developers to local.properties, so make those values
// available without making an emulator-only endpoint the default.
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

fun configuredValue(name: String): String? =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(name)?.takeIf { it.isNotBlank() }

val stagingRequested = gradle.startParameter.taskNames.any { it.contains("Staging", ignoreCase = true) }
val releaseRequested = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

fun configuredUrl(propertyName: String, environmentName: String): String {
    val value =
        configuredValue(propertyName)
            ?: throw GradleException(
                "$propertyName is required for the $environmentName build. " +
                    "Pass -P$propertyName=https://.../api/ or set the environment variable.",
            )
    val normalized = value.trim().trimEnd('/') + "/"
    if (!normalized.startsWith("https://")) {
        throw GradleException("$propertyName must use HTTPS for the $environmentName build")
    }
    return normalized
}

// The Android client has one production backend. Keep the fallback here so a
// release APK can never silently fall back to a retired backend endpoint.
fun configuredProductionApiUrl(): String {
    val configured =
        configuredValue("RELEASE_API_BASE_URL")
            ?: "https://backend-piyushcapitals-4171.vercel.app/api/"
    val normalized = configured.trim().trimEnd('/') + "/"
    if (!normalized.startsWith("https://")) {
        throw GradleException("RELEASE_API_BASE_URL must use HTTPS")
    }
    return normalized
}

fun configuredPublicValue(
    propertyName: String,
    environmentName: String,
    required: Boolean,
    vararg aliases: String,
): String {
    val configuredNames = listOf(propertyName) + aliases.toList()
    val value = configuredNames.asSequence()
        .mapNotNull(::configuredValue)
        .firstOrNull()
        ?: if (!required) "" else throw GradleException(
                "$propertyName is required for the $environmentName build. " +
                    "Pass -P$propertyName=... or set the environment variable.",
            )
    return value.trim()
}

fun configuredSupabaseUrl(propertyName: String, environmentName: String, required: Boolean): String {
    val value = configuredPublicValue(propertyName, environmentName, required)
    if (value.isNotEmpty() && !value.startsWith("https://")) {
        throw GradleException("$propertyName must use HTTPS for the $environmentName build")
    }
    return value.trimEnd('/')
}

val releaseStoreFile =
    configuredValue("RELEASE_KEYSTORE_FILE")?.let(::file)
        ?: keystoreProperties.getProperty("storeFile")?.let(::file)
val releaseStorePassword = configuredValue("RELEASE_KEYSTORE_PASSWORD") ?: keystoreProperties.getProperty("storePassword")
val releaseKeyAlias = configuredValue("RELEASE_KEY_ALIAS") ?: keystoreProperties.getProperty("keyAlias")
val releaseKeyPassword = configuredValue("RELEASE_KEY_PASSWORD") ?: keystoreProperties.getProperty("keyPassword")
val hasReleaseSigning =
    releaseStoreFile?.isFile == true &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.glowup.ai"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.glowup.ai"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "com.glowup.ai.HiltTestRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Development keeps the production application id for local deep-link testing.
            // applicationIdSuffix = ".debug"
            isDebuggable = true
            // LOCAL BACKEND - Configurable via local.properties
            // Add DEBUG_API_BASE_URL=http://YOUR_IP:8000/api/ to local.properties
            val configuredDebugApiUrl = configuredValue("DEBUG_API_BASE_URL")
                ?: "https://backend-piyushcapitals-4171.vercel.app/api/"
            // Retrofit requires a base URL ending in a slash. Treat a local
            // override the same way as staging/release URLs so an otherwise
            // valid `http://host:port/api` value does not crash at startup.
            val debugApiUrl = configuredDebugApiUrl.trim().trimEnd('/') + "/"
            if (!debugApiUrl.startsWith("http://") && !debugApiUrl.startsWith("https://")) {
                throw GradleException("DEBUG_API_BASE_URL must use HTTP or HTTPS")
            }
            buildConfigField("String", "API_BASE_URL", "\"$debugApiUrl\"")
            buildConfigField(
                "String",
                "SUPABASE_URL",
                "\"${configuredSupabaseUrl("DEBUG_SUPABASE_URL", "development", false)}\"",
            )
            buildConfigField(
                "String",
                "SUPABASE_ANON_KEY",
                "\"${configuredPublicValue("DEBUG_SUPABASE_ANON_KEY", "development", false, "SUPABASE_PUBLISHABLE_KEY")}\"",
            )
            buildConfigField("String", "SUPABASE_REDIRECT_URI", "\"glowup://auth/callback\"")
            buildConfigField(
                "String",
                "GOOGLE_WEB_CLIENT_ID",
                "\"${configuredPublicValue("DEBUG_GOOGLE_WEB_CLIENT_ID", "development", false)}\"",
            )
        }

        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            isDebuggable = false
            matchingFallbacks += listOf("debug")
            val stagingUrl =
                if (stagingRequested) {
                    configuredUrl("STAGING_API_BASE_URL", "staging")
                } else {
                    "https://staging.invalid/api/"
                }
            buildConfigField("String", "API_BASE_URL", "\"$stagingUrl\"")
            buildConfigField(
                "String",
                "SUPABASE_URL",
                "\"${configuredSupabaseUrl("STAGING_SUPABASE_URL", "staging", stagingRequested)}\"",
            )
            buildConfigField(
                "String",
                "SUPABASE_ANON_KEY",
                "\"${configuredPublicValue("STAGING_SUPABASE_ANON_KEY", "staging", stagingRequested, "SUPABASE_PUBLISHABLE_KEY")}\"",
            )
            buildConfigField("String", "SUPABASE_REDIRECT_URI", "\"glowup://auth/callback\"")
            buildConfigField(
                "String",
                "GOOGLE_WEB_CLIENT_ID",
                "\"${configuredPublicValue("STAGING_GOOGLE_WEB_CLIENT_ID", "staging", stagingRequested)}\"",
            )
            if (stagingRequested && !hasReleaseSigning) {
                throw GradleException(
                    "app/keystore.properties is required for the staging build; refusing debug signing",
                )
            }
            signingConfig = signingConfigs.getByName("release")
        }

        release {
            if (releaseRequested && !hasReleaseSigning) {
                throw GradleException(
                    "app/keystore.properties is required for the release build; refusing debug signing",
                )
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseUrl = if (releaseRequested) configuredProductionApiUrl() else "https://release.invalid/api/"
            buildConfigField("String", "API_BASE_URL", "\"$releaseUrl\"")
            buildConfigField(
                "String",
                "SUPABASE_URL",
                "\"${configuredSupabaseUrl("RELEASE_SUPABASE_URL", "production", releaseRequested)}\"",
            )
            buildConfigField(
                "String",
                "SUPABASE_ANON_KEY",
                "\"${configuredPublicValue("RELEASE_SUPABASE_ANON_KEY", "production", releaseRequested, "SUPABASE_PUBLISHABLE_KEY")}\"",
            )
            buildConfigField("String", "SUPABASE_REDIRECT_URI", "\"glowup://auth/callback\"")
            buildConfigField(
                "String",
                "GOOGLE_WEB_CLIENT_ID",
                "\"${configuredPublicValue("RELEASE_GOOGLE_WEB_CLIENT_ID", "production", releaseRequested)}\"",
            )
            signingConfig = signingConfigs.getByName("release")
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

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        checkReleaseBuilds = true
        ignoreWarnings = false
    }
}

kotlin {
    // Keep the compiler and CI on the same JDK. Bytecode remains Java 17
    // compatible through compileOptions above.
    jvmToolchain(25)
}

if (!hasReleaseSigning) {
    logger.warn("A complete release keystore is not available; release tasks will refuse to run")
}

/**
 * ML Kit loads CommonComponentRegistrar from manifest metadata by reflection before
 * MainActivity starts. R8's usage report lists removed members, so make the release
 * build fail if it ever strips the registrar constructor again.
 */
val verifyReleaseMlKitRegistrar by tasks.registering {
    dependsOn("minifyReleaseWithR8")
    doLast {
        val usage = layout.buildDirectory.file("outputs/mapping/release/usage.txt").get().asFile
        check(usage.isFile) { "R8 usage report was not produced for the release build" }
        val removedConstructor = Regex(
            """(?m)^com\\.google\\.mlkit\\.common\\.internal\\.CommonComponentRegistrar:\\s*\\R\\s+public void <init>\\(\\)""",
        )
        check(!removedConstructor.containsMatchIn(usage.readText())) {
            "R8 removed ML Kit's manifest-discovered CommonComponentRegistrar constructor"
        }
    }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    dependsOn(verifyReleaseMlKitRegistrar)
}

dependencies {
    implementation("com.android.billingclient:billing:8.0.0")
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
    // Native Google account chooser (Credential Manager) instead of browser OAuth redirects.
    implementation(libs.androidx.credentials.core)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
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

    // Vico (Compose charts)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.okhttp.mockwebserver)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
