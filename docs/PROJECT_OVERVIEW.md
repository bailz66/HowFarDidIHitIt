# SmackTrack — Project Overview

## Mission
A super simple Golf GPS app for Android. No fluff — just measure how far you hit the ball, track what club you used, record the weather, and see your stats over time.

## Why This Exists
Every golfer wants to know their real distances. Range markers lie, memory is unreliable, and most golf GPS apps are bloated with features nobody asked for. This app does one thing well: **tap, walk, tap, done.**

## Core Principles
- **Simplicity first** — every screen should be obvious without a tutorial
- **Offline-first** — core functionality works without internet (weather is a nice-to-have overlay)
- **Optional accounts** — Google Sign-in for cloud sync; core features work without any account
- **No ads** — clean, distraction-free experience
- **Privacy** — GPS data stays on-device by default; optional Firestore sync is user-isolated by UID

## Features

| Feature | Issue | Summary |
|---------|-------|---------|
| Shot Tracking (GPS) | #2 | Mark start/end positions with calibrated GPS, live distance while walking, club selection, foreground service for screen lock, 15-min timeout |
| Weather Integration | #3 | Record temperature, weather condition, wind via Open-Meteo API; wind-adjusted carry with TrackMan physics; manual wind control |
| Shot Analytics | #4 | Per-club distance stats, sparklines, scatter strips, trend analysis, wind-adjusted toggle, session summaries |
| Achievements | — | 12 categories × 5 tiers (60 total), gallery view, unlock banners |
| Cloud Sync | — | Google Sign-in via Credential Manager, Firestore persistence, offline fallback |
| Sharing | — | Canvas-rendered shot card PNG shared via FileProvider |
| Modern UI/UX | #5 | Material Design 3, Poppins/Roboto typography, premium animations |
| Test Automation | #6 | 348 test methods across 21 files, JUnit 5, parameterized boundary/validation tests |
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
| Data Storage | SharedPreferences (local) + Firestore (cloud sync) | Offline-first with optional cloud backup |
| Authentication | Firebase Auth + Credential Manager (Google Sign-in) | One-tap sign-in, no password management |
| Testing | JUnit 5 — 348 test methods across 21 files | Comprehensive boundary, validation, and logic coverage |
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
│       │   │   ├── domain/         # Models (Club, GpsCoordinate, Achievement, AchievementChecker)
│       │   │   ├── location/       # GPS calibration, haversine, wind calc, LocationProvider
│       │   │   ├── network/        # WeatherService, WeatherMapper
│       │   │   ├── data/           # ShotRepository, AchievementRepository, AuthManager, ShotSerialization
│       │   │   ├── service/        # ShotTrackingService (foreground service)
│       │   │   ├── ui/
│       │   │   │   ├── screen/     # Shot tracker, analytics, history, settings, achievements
│       │   │   │   ├── share/      # ShareUtil, ShotCardRenderer
│       │   │   │   └── theme/      # Material 3 theme, colors, typography
│       │   │   ├── validation/     # Shot, GPS, weather, timestamp validators
│       │   │   └── MainActivity.kt # Single-activity entry point with bottom nav
│       │   └── res/
│       │       ├── values/         # strings.xml, themes, colors
│       │       ├── drawable/       # Vector icons
│       │       └── font/           # Poppins font family
│       ├── test/                   # 21 test files, 348 test methods (JUnit 5)
│       └── androidTest/            # Instrumented tests
├── docs/                           # Project documentation
├── .github/
│   └── workflows/                  # CI/CD pipelines (ci.yml, release.yml)
├── firestore.rules                 # Production Firestore security rules
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
- [Bug Hunt Methodology](./BUG_HUNT.md)
