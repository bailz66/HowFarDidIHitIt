# Architecture

## Overview
The app follows a **single-activity MVVM architecture** using Jetpack Compose for the UI layer. One ViewModel (`ShotTrackerViewModel`) manages all application state. Persistence is handled through `ShotRepository` (SharedPreferences + Firestore). No DI framework — services are instantiated directly.

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  ┌─────────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ ShotTracker │ │ Analytics│ │   History     │  │
│  │   Screen    │ │  Screen  │ │    Screen     │  │
│  └──────┬──────┘ └────┬─────┘ └──────┬───────┘  │
│         │              │              │          │
│         └──────────────┼──────────────┘          │
│                        │                         │
│               ┌────────┴─────────┐               │
│               │ ShotTrackerView  │               │
│               │     Model        │               │
│               └──┬───┬───────┬───┘               │
├──────────────────┼───┼───────┼───────────────────┤
│            Services & Data Layer                 │
│  ┌───────────────┴┐ ┌┴──────────────┐           │
│  │LocationProvider│ │ WeatherService│           │
│  │ (GPS Flow)     │ │(Open-Meteo)   │           │
│  └────────────────┘ └───────────────┘           │
│  ┌────────────────┐ ┌───────────────┐           │
│  │ ShotRepository │ │  AuthManager  │           │
│  │(Prefs+Firestore│ │(Firebase Auth)│           │
│  └────────────────┘ └───────────────┘           │
│  ┌────────────────────────────────┐             │
│  │ ShotTrackingService            │             │
│  │ (Foreground service for GPS)   │             │
│  └────────────────────────────────┘             │
└─────────────────────────────────────────────────┘
```

## Layers

### UI Layer (Presentation)
- **Compose Screens** — declarative UI, no Fragments, single Activity with bottom nav
- **ShotTrackerViewModel** — single `AndroidViewModel` holds all UI state as `StateFlow`
- **UI State** — immutable data classes (`ShotTrackerUiState`, `ShotResult`, `AppSettings`)
- **Events** — user actions flow up to the ViewModel via function calls

### Services & Data Layer
- **LocationProvider** — wraps `FusedLocationProviderClient`, emits `Flow<LocationUpdate>` using `callbackFlow`
- **WeatherService** — singleton `object`, fetches weather from Open-Meteo via `HttpURLConnection` with timeouts
- **ShotRepository** — hybrid persistence: SharedPreferences (local) + Firestore (cloud sync when signed in)
- **AchievementRepository** — achievement state persistence with migration support
- **AuthManager** — Firebase Auth + Credential Manager for Google Sign-in
- **ShotTrackingService** — foreground service that keeps GPS alive when screen is locked during walking phase

## Key Patterns

### State Management
```kotlin
// ViewModel exposes immutable state
class ShotTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ShotTrackerUiState())
    val uiState: StateFlow<ShotTrackerUiState> = _uiState.asStateFlow()
}

// Screen observes state
@Composable
fun ShotTrackerScreen(viewModel: ShotTrackerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}
```

### Direct Service Instantiation
```kotlin
// LocationProvider created in ViewModel constructor — no DI framework
class ShotTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val locationProvider = LocationProvider(application)
}

// WeatherService is a Kotlin object singleton — called directly
val weather = WeatherService.fetchWeather(lat, lon)
```

### Navigation
```kotlin
// Bottom navigation with 4 tabs, managed in MainActivity
// Tabs: Tracker, Analytics, History, Settings
// No Jetpack Navigation library — simple when-based tab switching
```

## Data Flow

### Recording a Shot
```
User taps "SMACK"
  → ViewModel enters CALIBRATING_START phase
  → ShotTrackingService.start() → foreground notification appears
  → FLAG_KEEP_SCREEN_ON enabled
  → Shot timeout (15 min) countdown begins
  → LocationProvider starts emitting GPS updates via Flow
  → GPS samples collected over 3.5s at 500ms intervals
  → calibrateWeighted() applies accuracy gating + MAD outlier rejection
  → Start accuracy captured (estimatedAccuracyMeters)
  → ViewModel updates state: startCoordinate = calibrated position
  → Phase transitions to WALKING → live distance polling begins

User walks to ball... (GPS alive even with screen locked)
  → ViewModel polls currentLat/currentLon every 1s
  → Haversine distance calculated from startCoordinate
  → UI updates live distance display

User taps "TRACK"
  → ViewModel enters CALIBRATING_END phase
  → GPS calibration (2s) and WeatherService.fetchWeather() run in parallel via async
  → Final distance calculated via Haversine (clamped: NaN→0, >500yd→500)
  → End accuracy captured, combined with start accuracy (worst of two)
  → ShotResult created with distance + weather + GPS accuracy data
  → Shot persisted to SharedPreferences + Firestore (if signed in)
  → Achievements checked and unlocked
  → ShotTrackingService.stop() → notification dismissed
  → FLAG_KEEP_SCREEN_ON cleared, shot timeout cancelled
  → Phase transitions to RESULT → UI shows shot result

TIMEOUT (15 min, no TRACK):
  → Toast "Shot timed out after 15 minutes"
  → nextShot() → stops service, back to CLUB_SELECT
```

### Viewing Analytics
```
User navigates to Stats tab
  → Screen reads shotHistory from ViewModel state
  → Aggregates stats (avg, min, max, count) per club
  → Wind-adjusted distances calculated when toggle enabled
  → UI renders club cards with stats, sparklines, and scatter strips
```

## Threading Model
- **Main thread** — UI rendering, state observation
- **IO dispatcher** — weather API calls (`WeatherService.fetchWeather`)
- **viewModelScope** — GPS calibration, live distance polling
- All async work managed via `viewModelScope` coroutines

## Error Handling Strategy
- **GPS errors** — fallback to raw `currentLat/currentLon` if calibration fails
- **Network errors** — silent fallback to `WeatherData` with 70°F baseline (zero wind effect)
- **Distance anomalies** — NaN/infinite → 0 with toast; >500yd → capped at 500 with toast
- **JSON deserialization** — `optString()`/`optInt()`/`optDouble()` with safe defaults; per-shot try/catch prevents one corrupt record from losing all data
- **Locale safety** — `String.format(Locale.US, ...)` for API URL parameters prevents comma decimal separators
- **Permission denied** — `locationPermissionGranted` flag prevents GPS start; UI shows permission request
- **Firestore sync** — guarded by `if (uid != null)` checks; no sync calls without authentication; previous sync job cancelled before new one to prevent status races
- **State atomicity** — values captured from `_uiState.update {}` lambda, not re-read from `.value` after update, preventing concurrent overwrite bugs
- **Google Sign-In** — SHA-256 hashed nonce for replay attack prevention; generic error messages (no raw exception details shown to users)
- No crash-on-error — every error path has a graceful fallback

## Module Dependencies
```
:app (single module)
  ├── Google Play Services Location (GPS)
  ├── Firebase BOM 33.7.0
  │   ├── firebase-firestore-ktx (Firestore)
  │   └── firebase-auth-ktx (Firebase Auth)
  ├── Credential Manager + Google ID (Google Sign-in)
  ├── Jetpack Compose (via BOM) + Material 3
  ├── Lifecycle (AndroidViewModel, StateFlow)
  └── org.json (JSON parsing for weather, test-only JVM dep)
```
