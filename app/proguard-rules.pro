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

# Keep line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
