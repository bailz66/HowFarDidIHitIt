# SmackTrack - Golf Shot Distance

A GPS-powered golf distance tracking app for Android. Walk to your ball, tap to measure, and get accurate yardages with real-time wind analysis.

## Features

### Shot Tracking
- **GPS Calibration** — Accuracy-weighted averaging with outlier rejection for precise start/end positions
- **Live Distance** — Real-time yard/meter counter updates as you walk to your ball
- **Club Selection** — 18 clubs across 4 categories (Woods, Hybrids, Irons, Wedges) with the ability to change clubs mid-walk without losing GPS tracking

### Wind Analysis
- **Physics-Based Model** — Non-linear wind effect calculations calibrated against TrackMan launch monitor data
- **16-Point Direction** — Relative wind arrow showing exactly how wind affects your shot (helping, hurting, crosswind)
- **Carry Effect** — Estimated yards gained or lost due to wind, factoring in headwind/tailwind asymmetry
- **Lateral Displacement** — Crosswind push estimate in yards
- **Ball Flight Trajectory** — Low, Mid, or High settings that adjust wind exposure calculations

### Analytics
- **Per-Club Stats** — Average, longest, and shortest distances for every club in your bag
- **Wind-Adjusted Toggle** — Switch between raw and wind-adjusted distances to see your true carry
- **Club Drill-Down** — Tap any club for detailed shot-by-shot history with weather conditions
- **Time Period Filter** — Filter stats by 7 days, 30 days, 90 days, or all time

### History
- **Shot Log** — Chronological record of every shot with club, distance, weather, and wind data
- **Compact Wind Arrows** — Color-coded arrows showing wind impact at a glance

### Settings
- **Distance Units** — Yards or Meters
- **Wind Speed Units** — km/h or mph
- **Temperature Units** — Fahrenheit or Celsius
- **Ball Flight** — Low / Mid / High trajectory (affects wind calculations)
- **Club Bag** — Enable or disable individual clubs to match your bag

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM with StateFlow |
| **Typography** | Google Fonts (Poppins + Roboto tabular figures) |
| **GPS** | FusedLocationProviderClient (Google Play Services Location) with accuracy-weighted calibration |
| **Weather** | Open-Meteo API (free, no key required) via `HttpURLConnection` + `org.json` |
| **Wind Model** | Physics-based, calibrated against TrackMan data |
| **Testing** | JUnit 5 with parameterized boundary/validation tests |
| **CI/CD** | GitHub Actions |
| **Min SDK** | 33 (Android 13) |
| **Target SDK** | 36 (Android 16) |

## Project Structure

```
app/src/main/java/com/smacktrack/golf/
├── domain/                 # Data models
│   ├── Club.kt             # 18-club enum with categories and sort order
│   └── GpsCoordinate.kt    # Latitude/longitude data class
├── location/               # GPS and distance utilities
│   ├── GpsCalibrator.kt    # Accuracy-weighted GPS calibration with MAD outlier rejection
│   ├── HaversineCalculator.kt  # Great-circle distance and bearing
│   ├── LocationProvider.kt # FusedLocationProviderClient wrapper (Flow-based)
│   └── WindCalculator.kt   # Physics-based wind effect model
├── network/                # Weather data
│   ├── WeatherService.kt   # Open-Meteo API client (HttpURLConnection with timeouts)
│   └── WeatherMapper.kt    # WMO codes, compass directions, unit conversion, WeatherData model
├── ui/                     # Presentation layer
│   ├── ShotTrackerViewModel.kt  # Central state management
│   ├── screen/
│   │   ├── ShotTrackerScreen.kt  # Main tracking UI (5 phases)
│   │   ├── AnalyticsScreen.kt    # Club stats and drill-down
│   │   ├── HistoryScreen.kt      # Shot history log
│   │   └── SettingsScreen.kt     # User preferences
│   └── theme/
│       ├── ChipColors.kt    # Shared chip/gradient colors
│       ├── Color.kt          # WCAG AA-compliant palette
│       ├── Theme.kt          # Material 3 light theme
│       └── Type.kt           # Poppins + Roboto typography
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

## License

All rights reserved.
