# Feature 2: Weather Integration
> GitHub Issue: #3

## Purpose
Record weather conditions alongside each shot so the analytics can reveal patterns — do you hit shorter in cold weather? Does wind affect your driver distance? This is entirely passive — the golfer never interacts with weather directly.

## API: Open-Meteo

### Why Open-Meteo?
| Criteria | Open-Meteo |
|----------|-----------|
| Cost | Free |
| API Key | Not required |
| Signup | Not required |
| Rate Limits | Very generous (no concern for our use case) |
| Data Quality | Uses national weather services, good accuracy |
| Coverage | Global |

### Endpoint
```
GET https://api.open-meteo.com/v1/forecast
    ?latitude={lat}
    &longitude={lon}
    &current=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m
```

### Response
```json
{
  "latitude": 33.75,
  "longitude": -84.39,
  "current": {
    "time": "2026-02-17T14:00",
    "interval": 900,
    "temperature_2m": 22.5,
    "weather_code": 1,
    "wind_speed_10m": 12.3,
    "wind_direction_10m": 180
  }
}
```

## Data Captured Per Shot

| Field | API Source | Stored As | Display Example |
|-------|-----------|-----------|-----------------|
| Temperature (°C) | `current.temperature_2m` | `temperatureCelsius: Double` | "22°C" |
| Temperature (°F) | Converted from °C | Computed at display time | "72°F" |
| Weather condition | `current.weather_code` | `weatherCode: Int` | "Clear sky" |
| Wind speed | `current.wind_speed_10m` (km/h) | `windSpeedKmh: Double` | "8 mph" |
| Wind direction | `current.wind_direction_10m` (degrees) | `windDirectionDegrees: Int` | "NW" |

### Temperature Conversion
```kotlin
fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0
```

### WMO Weather Code Mapping
```kotlin
fun wmoCodeToLabel(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Foggy"
    48 -> "Depositing rime fog"
    51 -> "Light drizzle"
    // ... (full mapping in WeatherMapper.kt)
    95 -> "Thunderstorm"
    96 -> "Thunderstorm with slight hail"
    99 -> "Thunderstorm with heavy hail"
    else -> "Unknown"
}
```

### Wind Direction Mapping
```kotlin
fun degreesToCompass(degrees: Int): String {
    require(degrees in 0..360) { "Degrees must be 0-360, got $degrees" }
    val normalized = degrees % 360
    return when {
        normalized < 23  -> "N"
        normalized < 68  -> "NE"
        normalized < 113 -> "E"
        normalized < 158 -> "SE"
        normalized < 203 -> "S"
        normalized < 248 -> "SW"
        normalized < 293 -> "W"
        normalized < 338 -> "NW"
        else             -> "N"
    }
}
```

## Implementation

### WeatherService (singleton object)
Weather is fetched via `HttpURLConnection` with explicit timeouts to prevent indefinite hangs:

```kotlin
object WeatherService {
    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 10_000

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherData? =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                parseWeatherJson(json)
            } catch (_: Exception) {
                null
            }
        }

    internal fun parseWeatherJson(json: String): WeatherData? =
        try {
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            WeatherData(
                temperatureCelsius = current.getDouble("temperature_2m"),
                weatherCode = current.getInt("weather_code"),
                windSpeedKmh = current.getDouble("wind_speed_10m"),
                windDirectionDegrees = current.getInt("wind_direction_10m")
            )
        } catch (_: Exception) {
            null
        }
}
```

### Why No Caching?
- Weather is fetched once per shot (when the user taps "Mark End")
- The fetch runs in parallel with GPS calibration via `async`, adding zero latency
- A typical round produces ~18 API calls — well within Open-Meteo's rate limits
- Simpler code with no cache invalidation logic to maintain

## Error Handling

| Scenario | Behavior |
|----------|----------|
| No internet | Shot saves with fallback weather (zeroed values). No error shown to user. |
| API returns 500 | Same as no internet — silent fallback. |
| Connect timeout (> 5s) | Exception caught, returns null → fallback weather. |
| Read timeout (> 10s) | Exception caught, returns null → fallback weather. |
| Malformed response | `parseWeatherJson` returns null → fallback weather. |
| GPS not available | lat/lon are 0.0 — weather fetch skipped entirely. |

**Core principle:** Weather is a nice-to-have. It must NEVER block or delay shot tracking.

## Integration Points
- **Mark End** → `WeatherService.fetchWeather()` called in parallel with GPS calibration via `async`
- **Shot result** → weather data included in `ShotResult` for display
- **Analytics** → weather data enables filtering and pattern analysis
- **History** → compact wind arrows show wind impact at a glance

## Acceptance Criteria
- [x] Weather fetched from Open-Meteo when shot ends, using GPS coordinates
- [x] Temperature stored in Celsius, converted to Fahrenheit at display time
- [x] Weather condition mapped from WMO code to readable label
- [x] Wind speed (km/h) and direction (compass label) captured
- [x] Weather displayed on shot result screen
- [x] Offline/failure: shot saves with fallback weather — no error to user
- [x] Weather fetch never blocks shot tracking workflow (runs in parallel via async)
- [x] HTTP timeouts prevent indefinite hangs (5s connect, 10s read)
