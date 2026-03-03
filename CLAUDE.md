# Project: SmackTrack - Golf Shot Distance

Android golf GPS distance tracking app built with Jetpack Compose + Material 3.

## Product Philosophy

**Simplicity is the core of the product.** The three things that matter:

1. **Smack** — tap where you hit the ball
2. **Track** — walk to where it landed, tap to measure
3. **Club Selection** — pick your club so distances are grouped correctly

Everything else (achievements, analytics, cloud sync, sharing, weather) is supplementary. Never let extras complicate the core flow. The app should feel instant and obvious — no tutorials needed.

## Architecture

```
com.smacktrack.golf/
├── MainActivity.kt              # Single-activity, Scaffold + 3-tab nav
├── domain/
│   ├── Club.kt                  # 18 clubs (DRIVER→LOB_WEDGE), 4 categories
│   ├── GpsCoordinate.kt
│   ├── Achievement.kt           # 12 categories × 5 tiers = 60 achievements
│   └── AchievementChecker.kt    # Pure function, no side effects
├── location/
│   ├── LocationProvider.kt      # FusedLocationProviderClient wrapper
│   ├── GpsCalibrator.kt         # Accuracy-weighted multi-sample calibration
│   ├── HaversineCalculator.kt   # Haversine distance + bearing (lat/lon → meters/yards)
│   └── WindCalculator.kt        # TrackMan-calibrated wind physics
├── network/
│   ├── WeatherService.kt        # Open-Meteo API (locale-safe URL formatting)
│   └── WeatherMapper.kt         # WMO codes, compass, weatherGroup mapping
├── data/
│   ├── ShotRepository.kt        # Hybrid local (SharedPreferences) + Firestore persistence
│   ├── AchievementRepository.kt # Achievement persistence + migration
│   ├── AnalyticsTracker.kt      # Shot analytics event tracking
│   ├── AuthManager.kt           # Firebase Auth + Credential Manager
│   └── ShotSerialization.kt     # ShotResult ↔ JSON/Firestore (safe deserialization)
├── service/
│   └── ShotTrackingService.kt   # Foreground service for GPS during screen lock
├── validation/
│   └── ShotValidator.kt
└── ui/
    ├── ShotTrackerViewModel.kt  # Central state (StateFlow<UiState>)
    ├── ShotDisplayUtils.kt      # Formatting extensions (distances, wind, temp)
    ├── share/
    │   ├── ShareUtil.kt         # PNG → FileProvider → share intent
    │   └── ShotCardRenderer.kt  # Canvas-based shot card image
    ├── theme/
    │   ├── ChipColors.kt        # Shared chip/gradient colors
    │   ├── Color.kt             # WCAG AA-compliant palette
    │   ├── Theme.kt             # Material 3 light theme, Poppins font
    │   └── Type.kt              # Typography (Poppins + Roboto tabular figures)
    └── screen/
        ├── ShotTrackerScreen.kt     # Core: Smack → Walk → Track → Result
        ├── AnalyticsScreen.kt       # Club stats, trends, sparklines
        ├── HistoryScreen.kt         # Session-grouped shot history
        ├── SettingsScreen.kt        # Units, clubs, cloud sync, achievements
        ├── AchievementGallery.kt    # Achievement grid + detail dialog
        ├── ChartComponents.kt       # Reusable charts, counters, session cards
        └── DistanceChartView.kt     # Distance chart visualization
```

## Environment

- **Android Studio**: `F:\android-studio\`
- **JAVA_HOME**: `F:/android-studio/jbr` (must be set before every Gradle command)
- **Android SDK**: `C:\Users\User\AppData\Local\Android\Sdk\`
- **Shell**: Git Bash on Windows (use Unix paths with `/c/` prefix)

## Building

```bash
export JAVA_HOME="F:/android-studio/jbr"
cd /c/Users/User/AndroidStudioProjects/HowfardidIhitit2
./gradlew assembleDebug
```

## Running the Emulator

The working AVD is `Medium_Phone_API_36.1_2` (API 36, x86_64). The other AVD (`Medium_Phone_API_36.1`) is broken — missing system image.

### Start the emulator (run in background):

```bash
/c/Users/User/AppData/Local/Android/Sdk/emulator/emulator.exe -avd Medium_Phone_API_36.1_2 -no-snapshot-load &
```

### Wait for it to fully boot:

```bash
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe wait-for-device && \
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe shell "while [[ -z \$(getprop sys.boot_completed) ]]; do sleep 2; done" && \
echo "Emulator ready"
```

### Install and launch the app:

```bash
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -S -n com.smacktrack.golf/.MainActivity
```

### Restart app without reinstalling:

```bash
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -S -n com.smacktrack.golf/.MainActivity
```

### Check if emulator is running:

```bash
/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe devices
```

## Building Release

```bash
export JAVA_HOME="F:/android-studio/jbr"
./gradlew assembleRelease
```

## Running Tests

```bash
export JAVA_HOME="F:/android-studio/jbr"
./gradlew testDebugUnitTest
```

## Build Config

- **compileSdk**: 36, **minSdk**: 33, **targetSdk**: 36
- **Java**: VERSION_11, **Kotlin**: 2.0.21
- **Compose BOM**: 2024.09.00
- **Release signing**: env vars (KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
- **ProGuard**: minification + resource shrinking enabled for release, Log.v/d/i stripped

## Core Features

### Shot Flow (the product)
- **Smack page** — centered Smack button, last 3 recent shots, session summary card
- **Walking page** — live distance counter, club selection chips, Track button
- **Result page** — distance card with weather, wind arrow, wind-adjusted carry, GPS accuracy indicator, share button
- **GPS calibration** — accuracy-weighted multi-sample calibration; start 3.5s, end 2s
- **Foreground service** — keeps GPS alive when screen locks during walking phase (notification-only shell)
- **Screen keep-awake** — FLAG_KEEP_SCREEN_ON during active tracking phases
- **Shot timeout** — 15-minute auto-reset for abandoned shots
- **Distance clamping** — NaN/infinite → 0, >500yd → capped at 500 with toast warning

### Supplementary Features
- **History page** — session-grouped shot history with pagination, delete confirmation
- **Analytics (Stats tab)** — per-club AVG/LNG/SHT, sparklines, scatter strips, trend analysis, time period filtering, wind-adjusted toggle
- **Settings** — distance/wind/temperature units, trajectory (Low/Mid/High), club toggles
- **Achievements** — 12 categories × 5 tiers (Bronze→Diamond), gallery dialog, unlock banners
- **Cloud sync** — Google Sign-in, Firestore sync, local SharedPreferences fallback
- **Share** — Canvas-rendered shot card PNG shared via FileProvider
- **Weather** — Open-Meteo API, wind-adjusted carry (TrackMan-calibrated physics), manual wind direction/speed control

## Key Data Models

**ShotResult**: club, distanceYards, distanceMeters, weatherDescription, temperatureF/C, windSpeedKmh, windDirectionCompass, windDirectionDegrees, shotBearingDegrees, timestampMs

**AppSettings**: distanceUnit (YARDS/METERS), windUnit (KMH/MPH), temperatureUnit (F/C), trajectory (LOW/MID/HIGH with multiplier), enabledClubs

**Club**: 18 clubs in 4 categories (WOOD, HYBRID, IRON, WEDGE), each with displayName, sortOrder (1-18)

## Firebase / Firestore

- **Project**: `smashtrack-a2c99` (Firebase Console)
- **Plan**: Spark (free tier) — no billing required
- **Firestore**: production security rules (user-isolated by UID, increment-only global stats)
- **Auth**: Google Sign-in via Credential Manager API
- **`app/google-services.json`**: downloaded from Firebase Console — do NOT commit to public repos
- **Data structure**:
  - `users/{uid}/shots/{timestampMs}` — shot documents
  - `users/{uid}/settings/prefs` — settings document
  - `users/{uid}/achievements/{storageKey}` — achievement documents

### SHA-1 Fingerprints (registered in Firebase Console → Project settings → Your apps)

- **Debug**: `F5:20:03:5C:D2:FF:F7:A7:92:CB:06:C9:C6:69:B4:A6:BB:03:F6:C4` (from `~/.android/debug.keystore`, valid until 2056)
- **Release**: TODO — add release keystore SHA-1 before publishing to Play Store

To get a SHA-1:
```bash
"F:/android-studio/jbr/bin/keytool" -list -v -keystore /c/Users/User/.android/debug.keystore -alias androiddebugkey -storepass android
```

## Key Dependencies

- `play-services-location` 21.3.0 — GPS via FusedLocationProviderClient
- `firebase-firestore-ktx` / `firebase-auth-ktx` — Firestore + Firebase Auth (via BOM 33.7.0)
- `credentials-play-services-auth` 1.3.0 / `googleid` 1.1.1 — Credential Manager Google Sign-in
- `org.json:json` 20231013 — test-only dependency for WeatherService JSON parsing tests
- `junit5` 5.10.2 — unit testing framework

## CI/CD

### GitHub Actions (`.github/workflows/ci.yml`)
- Triggers: push to master/main/release/*, pull requests
- Jobs: lint → unit-test → instrumented-test (API 33 emulator) → build
- Artifacts: test reports, debug APK

### Release Pipeline (`.github/workflows/release.yml`)
- Triggers: tags matching `v*`
- Jobs: test → release (bundleRelease + assembleRelease)
- Creates GitHub release with auto-generated notes

## GitHub

- **Repo**: https://github.com/bailz66/HowFarDidIHitIt
- **CLI auth**: `gh` is installed at `/c/Users/User/gh/bin/gh.exe` and authenticated with workflow scope
