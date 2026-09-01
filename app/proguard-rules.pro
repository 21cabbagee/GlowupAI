# Add project specific ProGuard/R8 rules here.
# These rules are merged with the default rules AGP applies when
# optimization.enable = true (release build type) - see app/build.gradle.kts.
# Companion, source-set-discovered keep rules also live in
# app/src/main/keepRules/rules.keep (AGP combines everything under
# src/<variant>/keepRules automatically).
#
# https://developer.android.com/studio/build/shrink-code

# ---------------------------------------------------------------------------
# kotlinx.serialization
# Keep generated serializer companions/objects and @Serializable classes so
# reflection-free serialization still finds them after shrinking/obfuscation.
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `Companion` object of serializable classes and their `serializer()` method.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    <fields>;
}

# ---------------------------------------------------------------------------
# Retrofit / OkHttp
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.Response
-keep,allowobfuscation interface retrofit2.Call
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Hilt / Dagger
# ---------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}
-dontwarn dagger.hilt.**

# ---------------------------------------------------------------------------
# ML Kit face detection
# ---------------------------------------------------------------------------
-keep class com.google.mlkit.vision.face.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_face.** { *; }
-dontwarn com.google.mlkit.**

# ---------------------------------------------------------------------------
# Firebase
# ---------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ---------------------------------------------------------------------------
# App DTOs / domain models
# These are (de)serialized by kotlinx.serialization and/or persisted by Room,
# so field names and no-arg construction must survive shrinking/obfuscation.
# ---------------------------------------------------------------------------
-keep,includedescriptorclasses class com.glowup.ai.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.glowup.ai.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.glowup.ai.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.glowup.ai.data.remote.dto.** { *; }
-keep @kotlinx.serialization.Serializable class com.glowup.ai.domain.model.** { *; }
-keep class com.glowup.ai.data.remote.dto.** { <fields>; }
-keep class com.glowup.ai.domain.model.** { <fields>; }

# ---------------------------------------------------------------------------
# Compose - Keep all composable functions and ViewModels
# ---------------------------------------------------------------------------
-keep @androidx.compose.runtime.Composable class * { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ---------------------------------------------------------------------------
# Additional app classes that might be stripped
# ---------------------------------------------------------------------------
-keep class com.glowup.ai.** { *; }
-keep interface com.glowup.ai.** { *; }
-keep enum com.glowup.ai.** { *; }

# Keep all ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
    public <methods>;
}

# Keep navigation arguments
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.navigation.** { *; }

# Keep Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**

# ---------------------------------------------------------------------------
# Prevent crashes on startup - keep all app entry points
# ---------------------------------------------------------------------------
-keep class com.glowup.ai.GlowUpApplication { *; }
-keep class com.glowup.ai.MainActivity { *; }
-keep class com.glowup.ai.data.** { *; }
-keep class com.glowup.ai.feature.** { *; }
-keep class com.glowup.ai.core.** { *; }

# Keep all data classes with @Serializable
-keepclassmembers class com.glowup.ai.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Debugging - print what's being removed (comment out for production)
# -whyareyoukeeping class com.glowup.ai.**
