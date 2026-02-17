# Architecture

## Overview
The app follows a **single-activity MVVM architecture** using Jetpack Compose for the UI layer. This is the standard, Google-recommended architecture for modern Android apps.

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  ┌─────────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ ShotTracker │ │ Analytics│ │ Shot History  │  │
│  │   Screen    │ │  Screen  │ │    Screen     │  │
│  └──────┬──────┘ └────┬─────┘ └──────┬───────┘  │
│         │              │              │          │
│  ┌──────┴──────┐ ┌────┴─────┐ ┌──────┴───────┐  │
│  │ ShotTracker │ │Analytics │ │  History      │  │
│  │  ViewModel  │ │ViewModel │ │  ViewModel    │  │
│  └──────┬──────┘ └────┬─────┘ └──────┬───────┘  │
├─────────┼──────────────┼──────────────┼──────────┤
│         │         Domain Layer        │          │
│  ┌──────┴─────────────────────────────┴───────┐  │
│  │            ShotRepository                   │  │
│  └──────┬─────────────────────────────┬───────┘  │
│  ┌──────┴──────┐              ┌───────┴───────┐  │
│  │  Location   │              │   Weather     │  │
│  │  Service    │              │   Service     │  │
│  └─────────────┘              └───────────────┘  │
├──────────────────────────────────────────────────┤
│                   Data Layer                     │
│  ┌─────────────┐              ┌───────────────┐  │
│  │  Room DB    │              │ Retrofit      │  │
│  │  (ShotDao)  │              │ (WeatherApi)  │  │
│  └─────────────┘              └───────────────┘  │
└──────────────────────────────────────────────────┘
```

## Layers

### UI Layer (Presentation)
- **Compose Screens** — declarative UI, no Fragments
- **ViewModels** — hold UI state as `StateFlow`, survive configuration changes
- **UI State** — immutable data classes representing screen state
- **Events** — user actions flow up to ViewModels via function calls

### Domain Layer
- **Repository** — single source of truth for shot data, coordinates between local DB and weather API
- **Location Service** — wraps FusedLocationProviderClient, handles GPS calibration logic
- **Weather Service** — wraps Retrofit API calls, manages 1-hour cache

### Data Layer
- **Room Database** — local SQLite storage for shots
- **Retrofit Client** — HTTP client for Open-Meteo weather API
- **DAOs** — type-safe database queries

## Key Patterns

### State Management
```kotlin
// ViewModel exposes immutable state
class ShotTrackerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ShotTrackerUiState())
    val uiState: StateFlow<ShotTrackerUiState> = _uiState.asStateFlow()
}

// Screen observes state
@Composable
fun ShotTrackerScreen(viewModel: ShotTrackerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}
```

### Dependency Injection (Hilt)
```kotlin
@HiltAndroidApp
class HowFarApp : Application()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase { ... }

    @Provides @Singleton
    fun provideWeatherApi(): WeatherApi { ... }
}
```

### Navigation
```kotlin
// Type-safe routes
sealed class Screen(val route: String) {
    object ShotTracker : Screen("shot_tracker")
    object Analytics : Screen("analytics")
    object History : Screen("history")
}

// Bottom navigation with 3 tabs
```

## Data Flow

### Recording a Shot
```
User taps "Mark Start"
  → ViewModel starts GPS calibration coroutine
  → LocationService collects 5-10 GPS samples over 1.5s
  → Outliers rejected, average calculated
  → ViewModel updates state: startPin = calibratedCoordinate
  → UI shows live distance tracker

User walks to ball...
  → LocationService sends periodic location updates
  → ViewModel calculates Haversine distance from startPin
  → UI updates live distance display

User taps "Mark End"
  → Same GPS calibration as start
  → ViewModel calculates final distance
  → WeatherService provides cached temperature + conditions
  → Shot saved to Room DB via Repository
  → UI shows shot result
```

### Loading Analytics
```
User navigates to Analytics tab
  → ViewModel queries ShotDao with active filters
  → Room returns Flow<List<Shot>>
  → ViewModel aggregates stats (avg, min, max, count) per club
  → UI renders club cards with stats
```

## Threading Model
- **Main thread** — UI rendering, state observation
- **IO dispatcher** — Room queries, Retrofit calls, file I/O
- **Default dispatcher** — GPS calibration calculations, distance math
- All async work managed via `viewModelScope` coroutines

## Error Handling Strategy
- **GPS errors** — surface to UI with actionable messages ("Enable location services")
- **Network errors** — silent fallback, shots save with null weather data
- **Database errors** — should never happen in practice; log and surface generic error
- No crash-on-error — every error path has a graceful fallback

## Module Dependencies
```
:app (main module)
  ├── Google Play Services Location
  ├── Room (+ KSP for annotation processing)
  ├── Retrofit + OkHttp + Gson
  ├── Hilt (+ KSP)
  ├── Jetpack Compose (via BOM)
  ├── Compose Navigation
  └── Lifecycle (ViewModel, StateFlow)
```
