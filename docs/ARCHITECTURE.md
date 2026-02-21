# Architecture

## Overview
The app follows a **single-activity MVVM architecture** using Jetpack Compose for the UI layer. There is one ViewModel (`ShotTrackerViewModel`) that manages all application state in-memory. No database, no DI framework, no repository pattern.

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
│               └──┬───────────┬───┘               │
├──────────────────┼───────────┼───────────────────┤
│            Services Layer    │                   │
│  ┌───────────────┴───┐  ┌───┴───────────────┐   │
│  │ LocationProvider  │  │  WeatherService   │   │
│  │ (GPS Flow)        │  │  (Open-Meteo API) │   │
│  └───────────────────┘  └───────────────────┘   │
└─────────────────────────────────────────────────┘
```

## Layers

### UI Layer (Presentation)
- **Compose Screens** — declarative UI, no Fragments, single Activity with bottom nav
- **ShotTrackerViewModel** — single `AndroidViewModel` holds all UI state as `StateFlow`
- **UI State** — immutable data classes (`ShotTrackerUiState`, `ShotResult`, `AppSettings`)
- **Events** — user actions flow up to the ViewModel via function calls

### Services Layer
- **LocationProvider** — wraps `FusedLocationProviderClient`, emits `Flow<LocationUpdate>` using `callbackFlow`
- **WeatherService** — singleton `object`, fetches weather from Open-Meteo via `HttpURLConnection` with timeouts
- **No repository pattern** — the ViewModel interacts directly with services; data is in-memory only

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
User taps "Mark Start"
  → ViewModel enters CALIBRATING_START phase
  → LocationProvider starts emitting GPS updates via Flow
  → GPS samples collected over 2.5s at 500ms intervals
  → calibrateWeighted() applies accuracy gating + MAD outlier rejection
  → ViewModel updates state: startCoordinate = calibrated position
  → Phase transitions to WALKING → live distance polling begins

User walks to ball...
  → ViewModel polls currentLat/currentLon every 1s
  → Haversine distance calculated from startCoordinate
  → UI updates live distance display

User taps "Mark End"
  → ViewModel enters CALIBRATING_END phase
  → GPS calibration and WeatherService.fetchWeather() run in parallel via async
  → Final distance calculated via Haversine
  → ShotResult created with distance + weather data
  → Result added to in-memory shotHistory list
  → Phase transitions to RESULT → UI shows shot result
```

### Viewing Analytics
```
User navigates to Analytics tab
  → Screen reads shotHistory from ViewModel state
  → Aggregates stats (avg, min, max, count) per club in the composable
  → UI renders club cards with stats
```

## Threading Model
- **Main thread** — UI rendering, state observation
- **IO dispatcher** — weather API calls (`WeatherService.fetchWeather`)
- **viewModelScope** — GPS calibration, live distance polling
- All async work managed via `viewModelScope` coroutines

## Error Handling Strategy
- **GPS errors** — fallback to raw `currentLat/currentLon` if calibration fails
- **Network errors** — silent fallback to default `WeatherData` with zeroed fields
- **Permission denied** — `locationPermissionGranted` flag prevents GPS start; UI shows permission request
- No crash-on-error — every error path has a graceful fallback

## Module Dependencies
```
:app (single module)
  ├── Google Play Services Location (GPS)
  ├── Jetpack Compose (via BOM) + Material 3
  ├── Lifecycle (AndroidViewModel, StateFlow)
  └── org.json (JSON parsing for weather, test-only JVM dep)
```
