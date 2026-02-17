# Internationalization (i18n)
> GitHub Issue: #14

## Overview
The app is built from day one to support multiple languages. Even if we only ship English in v1, the architecture ensures adding new languages is a simple translation task — no code changes required.

## Core Principles

### 1. Never Hardcode User-Visible Strings
Every string the user sees must come from `res/values/strings.xml`, not from Kotlin code.

```kotlin
// BAD — hardcoded
Text("Mark Start")

// GOOD — string resource
Text(stringResource(R.string.btn_mark_start))
```

### 2. Store Data as Language-Neutral Keys
Database values must not contain display text. Store enum names or codes, resolve to display text at render time.

```kotlin
// Database stores: "DRIVER" (language-neutral)
// UI resolves: Club.DRIVER.displayName → stringResource(R.string.club_driver)

// Database stores: weatherCode = 61 (integer)
// UI resolves: weatherCodeToStringRes(61) → stringResource(R.string.weather_slight_rain)
```

### 3. Use Parameterized Strings for Dynamic Content
```xml
<!-- strings.xml -->
<string name="shot_distance">%1$s yards (%2$s m)</string>
<string name="shot_count">%1$d shots</string>
<string name="temperature_display">%1$.0f°F</string>
<string name="wind_display">%1$.0f mph %2$s</string>
```

```kotlin
stringResource(R.string.shot_distance, "245", "224")
// → "245 yards (224 m)"
```

### 4. Support Plurals
```xml
<plurals name="shot_count">
    <item quantity="one">%d shot</item>
    <item quantity="other">%d shots</item>
</plurals>
```

```kotlin
pluralStringResource(R.plurals.shot_count, count, count)
```

## String Resource Structure

### Default (English) — `res/values/strings.xml`
```xml
<resources>
    <!-- App -->
    <string name="app_name">How Far Did I Hit It</string>

    <!-- Shot Tracker -->
    <string name="btn_mark_start">Mark Start</string>
    <string name="btn_mark_end">Mark End</string>
    <string name="btn_reset">Reset</string>
    <string name="label_calibrating">Calibrating…</string>
    <string name="label_live_distance">Distance from start</string>
    <string name="label_select_club">Select Club</string>

    <!-- Club Names -->
    <string name="club_driver">Driver</string>
    <string name="club_wood_3">3 Wood</string>
    <string name="club_wood_5">5 Wood</string>
    <string name="club_wood_7">7 Wood</string>
    <string name="club_hybrid_3">3 Hybrid</string>
    <string name="club_hybrid_4">4 Hybrid</string>
    <string name="club_hybrid_5">5 Hybrid</string>
    <string name="club_iron_3">3 Iron</string>
    <string name="club_iron_4">4 Iron</string>
    <string name="club_iron_5">5 Iron</string>
    <string name="club_iron_6">6 Iron</string>
    <string name="club_iron_7">7 Iron</string>
    <string name="club_iron_8">8 Iron</string>
    <string name="club_iron_9">9 Iron</string>
    <string name="club_pitching_wedge">Pitching Wedge</string>
    <string name="club_gap_wedge">Gap Wedge</string>
    <string name="club_sand_wedge">Sand Wedge</string>
    <string name="club_lob_wedge">Lob Wedge</string>

    <!-- Club Categories -->
    <string name="category_woods">Woods</string>
    <string name="category_hybrids">Hybrids</string>
    <string name="category_irons">Irons</string>
    <string name="category_wedges">Wedges</string>

    <!-- Weather Conditions -->
    <string name="weather_clear_sky">Clear sky</string>
    <string name="weather_mainly_clear">Mainly clear</string>
    <string name="weather_partly_cloudy">Partly cloudy</string>
    <string name="weather_overcast">Overcast</string>
    <string name="weather_fog">Fog</string>
    <string name="weather_light_drizzle">Light drizzle</string>
    <string name="weather_moderate_drizzle">Moderate drizzle</string>
    <string name="weather_dense_drizzle">Dense drizzle</string>
    <string name="weather_slight_rain">Slight rain</string>
    <string name="weather_moderate_rain">Moderate rain</string>
    <string name="weather_heavy_rain">Heavy rain</string>
    <string name="weather_slight_snow">Slight snow</string>
    <string name="weather_moderate_snow">Moderate snow</string>
    <string name="weather_heavy_snow">Heavy snow</string>
    <string name="weather_rain_showers_light">Light rain showers</string>
    <string name="weather_rain_showers_moderate">Moderate rain showers</string>
    <string name="weather_rain_showers_heavy">Heavy rain showers</string>
    <string name="weather_thunderstorm">Thunderstorm</string>
    <string name="weather_thunderstorm_hail">Thunderstorm with hail</string>
    <string name="weather_unknown">Unknown</string>

    <!-- Units -->
    <string name="unit_yards">yards</string>
    <string name="unit_meters">m</string>
    <string name="unit_mph">mph</string>
    <string name="unit_kmh">km/h</string>

    <!-- Formats -->
    <string name="format_distance">%1$s %2$s (%3$s %4$s)</string>
    <string name="format_temperature_f">%1$.0f°F</string>
    <string name="format_temperature_c">%1$.0f°C</string>
    <string name="format_wind">%1$.0f %2$s %3$s</string>

    <!-- Analytics -->
    <string name="label_average">Average</string>
    <string name="label_longest">Longest</string>
    <string name="label_shortest">Shortest</string>
    <string name="label_shot_count">Shots</string>
    <string name="label_avg_temp">Avg. Temp</string>

    <!-- Filters -->
    <string name="filter_all_clubs">All Clubs</string>
    <string name="filter_last_7_days">Last 7 days</string>
    <string name="filter_last_30_days">Last 30 days</string>
    <string name="filter_last_90_days">Last 90 days</string>
    <string name="filter_all_time">All time</string>
    <string name="filter_clear_all">Clear all filters</string>

    <!-- Empty States -->
    <string name="empty_no_shots">Hit some shots to see your stats here!</string>
    <string name="empty_no_filter_results">No shots match your filters</string>

    <!-- Errors -->
    <string name="error_gps_required">Location permission is required to track shots</string>
    <string name="error_gps_low_accuracy">Low GPS signal — results may be less accurate</string>

    <!-- Navigation -->
    <string name="nav_tracker">Tracker</string>
    <string name="nav_analytics">Analytics</string>
    <string name="nav_history">History</string>
</resources>
```

### Adding a New Language
1. Create `res/values-{locale}/strings.xml` (e.g., `values-es` for Spanish)
2. Translate all strings
3. Android automatically selects the correct file based on device language
4. No code changes needed

### Example: Spanish — `res/values-es/strings.xml`
```xml
<resources>
    <string name="app_name">¿Qué tan lejos la pegué?</string>
    <string name="btn_mark_start">Marcar inicio</string>
    <string name="btn_mark_end">Marcar final</string>
    <string name="club_driver">Driver</string>
    <string name="club_iron_7">Hierro 7</string>
    <!-- ... etc ... -->
</resources>
```

## RTL (Right-to-Left) Support
- Use `start`/`end` instead of `left`/`right` in layouts
- Compose handles this automatically with `Modifier.padding(start = 16.dp)`
- Test with "Force RTL layout direction" in developer options
- Enabled in manifest: `android:supportsRtl="true"` (already set)

## Number & Date Formatting
- Use `NumberFormat.getInstance(locale)` for numbers
- Use `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)` for dates
- Distance units may vary by locale (yards in US/UK, meters elsewhere — future enhancement)

## Testing i18n
- Use pseudolocale in Android Studio to catch hardcoded strings
- Enable `resConfigs` in Gradle to include only supported locales in release builds
- Manually test with device language set to each supported locale
