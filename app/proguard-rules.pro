# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ── SmackTrack persistence & serialization ────────────────────────────────
# ShotRepository uses org.json to serialize these by field name.
# R8 must not rename or remove their fields/constructors.

-keep class com.smacktrack.golf.ui.ShotResult { *; }
-keep class com.smacktrack.golf.ui.AppSettings { *; }
-keep class com.smacktrack.golf.network.WeatherData { *; }

# Enums used in JSON serialization (stored by .name, resolved by valueOf)
-keepclassmembers enum com.smacktrack.golf.domain.Club { *; }
-keepclassmembers enum com.smacktrack.golf.ui.DistanceUnit { *; }
-keepclassmembers enum com.smacktrack.golf.ui.WindUnit { *; }
-keepclassmembers enum com.smacktrack.golf.ui.TemperatureUnit { *; }
-keepclassmembers enum com.smacktrack.golf.ui.Trajectory { *; }
-keepclassmembers enum com.smacktrack.golf.domain.AchievementTier { *; }
-keepclassmembers enum com.smacktrack.golf.domain.AchievementCategory { *; }

# Keep line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Foreground service ────────────────────────────────────────────────────
-keep class com.smacktrack.golf.service.ShotTrackingService { *; }

# ── Strip verbose/debug/info logs in release (keep warn/error for diagnostics) ──
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ── Firebase / Google Sign-in ─────────────────────────────────────────────
# Firebase and GMS ship their own consumer ProGuard rules.
# Only keep Credentials API classes needed for Google Sign-in via Credential Manager.
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
