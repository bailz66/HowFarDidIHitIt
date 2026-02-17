# How Far Did I Hit It — Project Overview

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
| Weather Integration | #3 | Record temperature, weather condition (rain/fog/clear), and wind via Open-Meteo API, cached hourly |
| Shot Analytics | #4 | Per-club distance stats, shot history, filtering by club/date/weather/temperature |
| Modern UI/UX | #5 | Material Design 3, dynamic color, responsive layouts, polished animations |
| Test Automation | #6 | Unit, integration, and UI test framework with CI |
| Deployment Pipeline | #10 | CI/CD, signing, Play Store release process |
| Internationalization | #14 | String externalization, RTL support, multi-language readiness |

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
| Location | FusedLocationProviderClient (Google Play Services) | Best accuracy, handles GPS/network fusion |
| Database | Room (SQLite) | Type-safe local persistence, reactive queries with Flow |
| HTTP Client | Retrofit + OkHttp | Lightweight, well-tested, standard for Android |
| Architecture | Single-activity, MVVM (ViewModel + StateFlow) | Simple, testable, follows Android best practices |
| Navigation | Jetpack Compose Navigation | Type-safe, integrated with Compose lifecycle |
| DI | Hilt | Standard DI for Android, reduces boilerplate |
| Testing | JUnit 5 + Espresso + Compose UI Testing | Comprehensive coverage across all layers |
| CI/CD | GitHub Actions | Free for public repos, good Android support |
| Min SDK | 26 (Android 8.0) | Covers 95%+ of active devices |
| Target SDK | 36 | Latest stable API level |

## Repository Structure
```
HowFarDidIHitIt/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/howfardidihitit/
│       │   │   ├── data/           # Room DB, DAOs, repositories
│       │   │   ├── domain/         # Models, use cases
│       │   │   ├── location/       # GPS calibration, location services
│       │   │   ├── network/        # Retrofit, weather API
│       │   │   ├── ui/
│       │   │   │   ├── components/ # Reusable composables
│       │   │   │   ├── screens/    # Shot tracker, analytics, history
│       │   │   │   ├── navigation/ # Nav graph, routes
│       │   │   │   └── theme/      # Material 3 theme, colors, typography
│       │   │   └── di/            # Hilt modules
│       │   └── res/
│       │       ├── values/         # strings.xml (default English)
│       │       ├── values-es/      # Spanish strings
│       │       ├── values-fr/      # French strings (etc.)
│       │       └── ...
│       ├── test/                   # Unit tests
│       └── androidTest/            # Instrumented + UI tests
├── docs/                           # Project documentation
├── .github/
│   └── workflows/                  # CI/CD pipelines
└── gradle/
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
- [Internationalization](./INTERNATIONALIZATION.md)
- [UI/UX Design](./UI_DESIGN.md)
