# SmackTrack - Golf Shot Distance

A GPS-powered golf distance tracking app for Android. Walk to your ball, tap to measure, and get accurate yardages with real-time wind analysis.

## Features

### Shot Tracking
- **Adaptive GPS Calibration** — Smart accuracy-weighted averaging (2-7s) that waits for GPS convergence with real-time accuracy feedback
- **Live Distance** — Real-time yard/meter counter updates as you walk to your ball
- **Club Selection** — 18 clubs across 4 categories (Woods, Hybrids, Irons, Wedges) with the ability to change clubs mid-walk without losing GPS tracking
- **Foreground Service** — GPS stays alive when screen locks during walking phase
- **Screen Keep-Awake** — Screen stays on during active tracking phases
- **Shot Timeout** — 15-minute auto-reset for abandoned shots
- **GPS Accuracy Indicator** — Shows measurement precision on result screen (warns if >15m)
- **Distance Clamping** — NaN/infinite distances handled gracefully, >500yd capped with warning

### Wind Analysis
- **Physics-Based Model** — Non-linear wind effect calculations calibrated against TrackMan launch monitor data
- **16-Point Direction** — Relative wind arrow showing exactly how wind affects your shot (helping, hurting, crosswind)
- **Carry Effect** — Estimated yards gained or lost due to wind, factoring in headwind/tailwind asymmetry
- **Lateral Displacement** — Crosswind push estimate in yards
- **Ball Flight Trajectory** — Low, Mid, or High settings that adjust wind exposure calculations
- **Manual Wind Control** — Adjust wind direction and speed on the result screen

### Analytics
- **Per-Club Stats** — Average, longest, and shortest distances for every club in your bag
- **Wind-Adjusted Toggle** — Switch between raw and wind-adjusted distances to see your true carry
- **Club Drill-Down** — Tap any club for detailed shot-by-shot history with weather conditions
- **Time Period Filter** — Filter stats by 7 days, 30 days, 90 days, or all time
- **Sparklines & Scatter Strips** — Visual trends for each club
- **Session Summary Cards** — Grouped shot summaries per playing session

### History
- **Shot Log** — Session-grouped chronological record of every shot with club, distance, weather, and wind data
- **Compact Wind Arrows** — Color-coded arrows showing wind impact at a glance
- **Pagination** — Efficient loading for large shot histories
- **Delete Confirmation** — Swipe-to-delete with confirmation dialog

### Achievements
- **12 Categories** — Distance milestones, hot streaks, consistency, variety, and more
- **5 Tiers** — Bronze, Silver, Gold, Platinum, Diamond progression
- **Gallery View** — Grid display with detail dialogs and unlock banners
- **60 Total Achievements** — Tracked automatically as you play

### Cloud Sync
- **Google Sign-in** — Via Credential Manager API (optional, no account required)
- **Firestore Sync** — Shots, settings, and achievements synced across devices
- **Offline First** — Full functionality without internet, SharedPreferences fallback
- **Production Security Rules** — User data isolated by UID

### Settings
- **Distance Units** — Yards or Meters
- **Wind Speed Units** — km/h or mph
- **Temperature Units** — Fahrenheit or Celsius
- **Ball Flight** — Low / Mid / High trajectory (affects wind calculations)
- **Club Bag** — Enable or disable individual clubs to match your bag

### Sharing
- **Shot Card Image** — Canvas-rendered PNG with distance, club, and weather data
- **Share Intent** — Share via any app using FileProvider

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM with StateFlow |
| **Typography** | Google Fonts (Poppins + Roboto tabular figures) |
| **GPS** | FusedLocationProviderClient (Google Play Services Location) with accuracy-weighted calibration |
| **Persistence** | SharedPreferences (local) + Firestore (cloud sync) |
| **Auth** | Firebase Auth + Credential Manager (Google Sign-in) |
| **Weather** | Open-Meteo API (free, no key required) via `HttpURLConnection` + `org.json` |
| **Wind Model** | Physics-based, calibrated against TrackMan data |
| **Testing** | JUnit 5 — 462 test executions across 21 test files |
| **CI/CD** | GitHub Actions (lint → unit-test → instrumented-test → build) |
| **Min SDK** | 33 (Android 13) |
| **Target SDK** | 36 (Android 16) |

## Project Structure

```
app/src/main/java/com/smacktrack/golf/
├── domain/                 # Data models
│   ├── Club.kt             # 18-club enum with categories and sort order
│   ├── GpsCoordinate.kt    # Latitude/longitude data class
│   ├── Achievement.kt      # 12 categories × 5 tiers = 60 achievements
│   └── AchievementChecker.kt  # Pure function, no side effects
├── location/               # GPS and distance utilities
│   ├── GpsCalibrator.kt    # Accuracy-weighted GPS calibration with MAD outlier rejection
│   ├── HaversineCalculator.kt  # Great-circle distance and bearing
│   ├── LocationProvider.kt # FusedLocationProviderClient wrapper (Flow-based)
│   └── WindCalculator.kt   # Physics-based wind effect model
├── network/                # Weather data
│   ├── WeatherService.kt   # Open-Meteo API client (locale-safe URL formatting)
│   └── WeatherMapper.kt    # WMO codes, compass directions, weatherGroup mapping
├── data/                   # Persistence
│   ├── ShotRepository.kt   # Hybrid local (SharedPreferences) + Firestore persistence
│   ├── AchievementRepository.kt  # Achievement persistence + migration
│   ├── AnalyticsTracker.kt # Shot analytics event tracking
│   ├── AuthManager.kt      # Firebase Auth + Credential Manager
│   └── ShotSerialization.kt  # ShotResult ↔ JSON/Firestore (safe deserialization)
├── service/                # Background services
│   └── ShotTrackingService.kt  # Foreground service for GPS during screen lock
├── ui/                     # Presentation layer
│   ├── ShotTrackerViewModel.kt  # Central state management
│   ├── ShotDisplayUtils.kt # Formatting extensions (distances, wind, temp)
│   ├── screen/
│   │   ├── ShotTrackerScreen.kt  # Main tracking UI (5 phases)
│   │   ├── AnalyticsScreen.kt    # Club stats, sparklines, trends
│   │   ├── HistoryScreen.kt      # Session-grouped shot history
│   │   ├── SettingsScreen.kt     # Units, clubs, cloud sync, achievements
│   │   ├── AchievementGallery.kt # Achievement grid + detail dialog
│   │   ├── ChartComponents.kt    # Reusable charts, counters, session cards
│   │   └── DistanceChartView.kt  # Distance chart visualization
│   ├── share/
│   │   ├── ShareUtil.kt    # PNG → FileProvider → share intent
│   │   └── ShotCardRenderer.kt  # Canvas-based shot card image
│   └── theme/
│       ├── ChipColors.kt   # Shared chip/gradient colors
│       ├── Color.kt         # WCAG AA-compliant palette
│       ├── Theme.kt         # Material 3 light theme
│       └── Type.kt          # Poppins + Roboto typography
├── validation/             # Data validation
│   └── ShotValidator.kt    # Coordinate, distance, weather, timestamp checks
└── MainActivity.kt         # Single-activity entry point with bottom nav
```

## Building

### Prerequisites
- Android Studio with Android SDK
- JDK 11+ (bundled JBR recommended)

### Build and Install
```bash
export JAVA_HOME="<path-to-jbr>"
./gradlew assembleDebug
./gradlew installDebug
```

### Run Tests
```bash
./gradlew testDebugUnitTest
```

## Wind Calculation Model

The wind effect calculator uses a non-linear physics model calibrated against TrackMan launch monitor data:

| Condition | Formula |
|-----------|---------|
| **Headwind** | `speed^1.3 x 0.8 x trajectory x (distance/150)` |
| **Tailwind** | `speed^1.1 x 0.4 x trajectory x (distance/150)` |
| **Crosswind** | `1 ft lateral per mph per 100 yds` |

Key physics principles:
- Aerodynamic drag scales with velocity squared — headwind increases apparent airspeed, causing disproportionately more drag
- Tailwind reduces airspeed but also reduces lift, causing the ball to descend sooner (headwind hurts ~2x more than tailwind helps)
- Higher ball flights spend more time in the air, increasing wind exposure

**Trajectory multipliers:** Low = 0.75, Mid = 1.0, High = 1.3

## GPS Calibration Algorithm

Position accuracy is improved through a multi-step calibration process:

1. **Cold-start rejection** — First GPS sample is discarded (often inaccurate)
2. **Accuracy gating** — Samples with reported accuracy > 20m are rejected
3. **Inverse-variance weighting** — More accurate samples receive higher weight (w = 1/accuracy^2)
4. **MAD outlier rejection** — Distances from weighted centroid are checked against median absolute deviation x 2.5
5. **Final weighted average** — Recomputed from inliers only with estimated accuracy

## Security Hardening

- **Locale-safe API URLs** — `String.format(Locale.US, ...)` prevents comma decimal separators breaking Open-Meteo API calls
- **Safe JSON deserialization** — `optString()`/`optInt()`/`optDouble()` with defaults; per-shot try/catch prevents one corrupt record from losing all data
- **ProGuard log stripping** — `Log.v`, `Log.d`, and `Log.i` stripped from release builds
- **Firestore security rules** — User data isolated by UID; global stats counter increment-only; schema validation on all writes
- **Distance validation** — NaN/infinite → 0, >500yd capped; prevents GPS anomalies from corrupting data
- **Weather validation** — NaN and Infinity guard on API responses
- **Firestore sync guards** — No sync calls without authenticated user
- **Firebase App Check** — Play Integrity API prevents API abuse
- **HTTPS enforced** — `cleartextTrafficPermitted="false"` in network security config
- **Release keystore** — Stored securely outside repo; CI/CD decodes from GitHub Secrets
- **In-app analytics opt-out** — Users can disable Firebase Analytics in Settings
- **Account deletion** — Full Firestore + Firebase Auth data deletion from within the app

## Privacy

- **Privacy Policy**: [https://bailz66.github.io/HowFarDidIHitIt/privacy-policy.html](https://bailz66.github.io/HowFarDidIHitIt/privacy-policy.html)
- **Data Deletion**: [https://bailz66.github.io/HowFarDidIHitIt/data-deletion.html](https://bailz66.github.io/HowFarDidIHitIt/data-deletion.html)

## Documentation

Detailed project documentation is available in the `docs/` directory:

- [Project Overview](docs/PROJECT_OVERVIEW.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Data Model](docs/DATA_MODEL.md)
- [UI Design](docs/UI_DESIGN.md)
- [Testing Strategy](docs/TESTING.md)
- [Deployment](docs/DEPLOYMENT.md)
- [Code Quality](docs/CODE_QUALITY.md)
- [Branching Strategy](docs/BRANCHING_STRATEGY.md)
- [Bug Hunt Methodology](docs/BUG_HUNT.md)
- [Versioning Strategy](docs/VERSIONING.md)

## License

All rights reserved.
