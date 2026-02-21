# SmackTrack — Project Overview

## Mission
A super simple Golf GPS app for Android. No fluff — just measure how far you hit the ball, track what club you used, record the weather, and see your stats over time.

## Why This Exists
Every golfer wants to know their real distances. Range markers lie, memory is unreliable, and most golf GPS apps are bloated with features nobody asked for. This app does one thing well: **tap, walk, tap, done.**

## Core Principles
- **Simplicity first** — every screen should be obvious without a tutorial
- **Offline-first** — core functionality works without internet (weather is a nice-to-have overlay)
- **No accounts** — no signup, no login, no cloud sync, no backend
- **No ads** — clean, distraction-free experience
- **Privacy** — GPS data stays on-device; weather API calls are anonymous

## Features

| Feature | Issue | Summary |
|---------|-------|---------|
| Shot Tracking (GPS) | #2 | Mark start/end positions with calibrated GPS, live distance while walking, club selection |
| Weather Integration | #3 | Record temperature, weather condition (rain/fog/clear), and wind via Open-Meteo API |
| Shot Analytics | #4 | Per-club distance stats, shot history, filtering by club/date/weather/temperature |
| Modern UI/UX | #5 | Material Design 3, Poppins/Roboto typography, responsive layouts |
| Test Automation | #6 | Unit tests with JUnit 5, parameterized boundary/validation tests, CI |
| Deployment Pipeline | #10 | CI/CD via GitHub Actions, signing, Play Store release process |

## Target Audience
- Casual and amateur golfers who want to learn their real distances
- Golfers who don't want to pay for a subscription GPS app
- Anyone who hits balls at a range and wants to track improvement

## Non-Goals
- No course maps or hole layouts
- No shot shape / draw / fade tracking
- No social features or leaderboards
- No GPS navigation or turn-by-turn to the green
- No monetization in v1

## Tech Stack

| Layer | Choice | Rationale |
|-------|--------|-----------|
| Language | Kotlin | Standard for modern Android development |
| UI Framework | Jetpack Compose + Material Design 3 | Declarative, modern, Google-recommended |
| GPS | FusedLocationProviderClient (Google Play Services Location) | Best accuracy, handles GPS/network fusion |
| Weather | Open-Meteo API via `HttpURLConnection` + `org.json` | Free, no key required, global coverage |
| Architecture | Single-activity, MVVM (AndroidViewModel + StateFlow) | Simple, testable, follows Android best practices |
| Data Storage | In-memory (no persistence) | Simplicity for v1; data resets on app restart |
| Testing | JUnit 5 with parameterized boundary/validation tests | Comprehensive coverage of business logic |
| CI/CD | GitHub Actions | Free for public repos, good Android support |
| Min SDK | 33 (Android 13) | Leverages modern APIs and permissions model |
| Target SDK | 36 | Latest stable API level |

## Repository Structure
```
HowFarDidIHitIt/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/smacktrack/golf/
│       │   │   ├── domain/         # Models (Club enum, GpsCoordinate)
│       │   │   ├── location/       # GPS calibration, haversine, wind calc, LocationProvider
│       │   │   ├── network/        # WeatherService, WeatherMapper, WeatherData
│       │   │   ├── ui/
│       │   │   │   ├── screen/     # Shot tracker, analytics, history, settings screens
│       │   │   │   └── theme/      # Material 3 theme, colors, typography
│       │   │   ├── validation/     # Shot, GPS, weather, timestamp validators
│       │   │   └── MainActivity.kt # Single-activity entry point with bottom nav
│       │   └── res/
│       │       ├── values/         # strings.xml, themes
│       │       ├── drawable/       # Vector icons
│       │       └── font/           # Poppins font family
│       ├── test/                   # Unit tests (JUnit 5)
│       └── androidTest/            # Instrumented tests
├── docs/                           # Project documentation
├── .github/
│   └── workflows/                  # CI/CD pipelines (ci.yml, release.yml)
└── gradle/
    └── libs.versions.toml          # Version catalog
```

## Related Documentation
- [Architecture](./ARCHITECTURE.md)
- [Data Model](./DATA_MODEL.md)
- [Code Quality](./CODE_QUALITY.md)
- [Feature 1: Shot Tracking](./FEATURE_1_SHOT_TRACKING.md)
- [Feature 2: Weather](./FEATURE_2_WEATHER.md)
- [Feature 3: Analytics](./FEATURE_3_ANALYTICS.md)
- [Testing Strategy](./TESTING.md)
- [Deployment & Play Store](./DEPLOYMENT.md)
- [UI/UX Design](./UI_DESIGN.md)
- [Branching Strategy](./BRANCHING_STRATEGY.md)
