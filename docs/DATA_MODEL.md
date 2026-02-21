# Data Model

## Overview
All data is held **in-memory** within `ShotTrackerViewModel`. There is no database, no persistence, and no cloud sync. Data resets when the app is restarted. This is intentional for v1 simplicity.

## State Container

```kotlin
data class ShotTrackerUiState(
    val phase: ShotPhase = ShotPhase.CLUB_SELECT,
    val selectedClub: Club? = Club.DRIVER,
    val startCoordinate: GpsCoordinate? = null,
    val liveDistanceYards: Int = 0,
    val liveDistanceMeters: Int = 0,
    val shotResult: ShotResult? = null,
    val shotHistory: List<ShotResult> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val locationPermissionGranted: Boolean = false
)
```

This is the single source of truth for the entire app, exposed as `StateFlow<ShotTrackerUiState>` from the ViewModel.

## Shot Result

```kotlin
data class ShotResult(
    val club: Club,
    val distanceYards: Int,
    val distanceMeters: Int,
    val weatherDescription: String,
    val temperatureF: Int,
    val temperatureC: Int,
    val windSpeedKmh: Double,
    val windDirectionCompass: String,
    val windDirectionDegrees: Int = 0,
    val shotBearingDegrees: Double = 0.0
)
```

| Field | Type | Description |
|-------|------|-------------|
| `club` | Club | The club used for this shot |
| `distanceYards` | Int | Haversine distance in yards (rounded) |
| `distanceMeters` | Int | Haversine distance in meters (rounded) |
| `weatherDescription` | String | WMO code mapped to label (e.g., "Clear sky") |
| `temperatureF` | Int | Temperature in Fahrenheit (rounded) |
| `temperatureC` | Int | Temperature in Celsius (rounded) |
| `windSpeedKmh` | Double | Wind speed in km/h |
| `windDirectionCompass` | String | Compass direction (e.g., "NW", "SE") |
| `windDirectionDegrees` | Int | Wind direction in degrees (0-360) |
| `shotBearingDegrees` | Double | Bearing from start to end position |

## Weather Data

```kotlin
data class WeatherData(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int
)
```

Returned by `WeatherService.fetchWeather()`. Maps directly to the Open-Meteo API `current` object fields.

## GPS Data Classes

```kotlin
data class GpsSample(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Double,
    val timestampMs: Long = System.currentTimeMillis()
)

data class CalibratedPosition(
    val coordinate: GpsCoordinate,
    val estimatedAccuracyMeters: Double,
    val sampleCount: Int
)

data class LocationUpdate(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
    val timestampMs: Long
)

data class GpsCoordinate(
    val lat: Double,
    val lon: Double
)
```

| Class | Source | Purpose |
|-------|--------|---------|
| `GpsSample` | GPS calibration | Raw sample with accuracy for weighted averaging |
| `CalibratedPosition` | `calibrateWeighted()` | Final calibrated position with accuracy estimate |
| `LocationUpdate` | `LocationProvider` | Raw FusedLocation fix emitted via Flow |
| `GpsCoordinate` | Domain model | Simple lat/lon pair used throughout the app |

## Settings

```kotlin
data class AppSettings(
    val distanceUnit: DistanceUnit = DistanceUnit.YARDS,
    val windUnit: WindUnit = WindUnit.MPH,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val trajectory: Trajectory = Trajectory.MID,
    val enabledClubs: Set<Club> = Club.entries.toSet()
)
```

Settings are in-memory and reset to defaults on app restart.

## Enums

### Shot Phase
```kotlin
enum class ShotPhase {
    CLUB_SELECT,        // Initial state — pick a club
    CALIBRATING_START,  // Collecting GPS samples for start position
    WALKING,            // Live distance tracking
    CALIBRATING_END,    // Collecting GPS samples for end position
    RESULT              // Shot result displayed
}
```

### Club
```kotlin
enum class Club(val displayName: String, val category: String, val sortOrder: Int) {
    DRIVER("Driver", "Woods", 1),
    WOOD_3("3 Wood", "Woods", 2),
    WOOD_5("5 Wood", "Woods", 3),
    WOOD_7("7 Wood", "Woods", 4),
    HYBRID_3("3 Hybrid", "Hybrids", 5),
    HYBRID_4("4 Hybrid", "Hybrids", 6),
    HYBRID_5("5 Hybrid", "Hybrids", 7),
    IRON_3("3 Iron", "Irons", 8),
    IRON_4("4 Iron", "Irons", 9),
    IRON_5("5 Iron", "Irons", 10),
    IRON_6("6 Iron", "Irons", 11),
    IRON_7("7 Iron", "Irons", 12),
    IRON_8("8 Iron", "Irons", 13),
    IRON_9("9 Iron", "Irons", 14),
    PITCHING_WEDGE("Pitching Wedge", "Wedges", 15),
    GAP_WEDGE("Gap Wedge", "Wedges", 16),
    SAND_WEDGE("Sand Wedge", "Wedges", 17),
    LOB_WEDGE("Lob Wedge", "Wedges", 18);
}
```

The `sortOrder` field ensures clubs always display in natural order: Driver at top, Lob Wedge at bottom.

## Data Flow Summary

```
GPS Fix → LocationUpdate → GpsSample → calibrateWeighted() → CalibratedPosition
                                                                    ↓
                                                              GpsCoordinate
                                                                    ↓
                                                         haversineMeters()
                                                                    ↓
                                                              ShotResult ← WeatherData
                                                                    ↓
                                                         ShotTrackerUiState.shotHistory
```

## Future: Persistence
When persistence is added (v2+), the in-memory `ShotResult` list would be replaced with Room database storage. The `ShotResult` data class maps naturally to a Room `@Entity`.
