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
    &current_weather=true
```

### Response
```json
{
  "latitude": 33.75,
  "longitude": -84.39,
  "current_weather": {
    "temperature": 22.5,
    "windspeed": 12.3,
    "winddirection": 180,
    "weathercode": 1,
    "time": "2026-02-17T14:00"
  }
}
```

## Data Captured Per Shot

| Field | API Source | Stored As | Display Example |
|-------|-----------|-----------|-----------------|
| Temperature (°C) | `current_weather.temperature` | `temperatureC: Double?` | "22°C" |
| Temperature (°F) | Converted from °C | `temperatureF: Double?` | "72°F" |
| Weather condition | `current_weather.weathercode` | `weatherCode: Int?` + `weatherCondition: String?` | "Clear sky" |
| Wind speed | `current_weather.windspeed` (km/h) | `windSpeedMph: Double?` | "8 mph" |
| Wind direction | `current_weather.winddirection` (degrees) | `windDirectionDegrees: Int?` + `windDirectionLabel: String?` | "NW" |

### Temperature Conversion
```kotlin
fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0
```

### WMO Weather Code Mapping
```kotlin
fun weatherCodeToCondition(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51 -> "Light drizzle"
    53 -> "Moderate drizzle"
    55 -> "Dense drizzle"
    61 -> "Slight rain"
    63 -> "Moderate rain"
    65 -> "Heavy rain"
    71 -> "Slight snow"
    73 -> "Moderate snow"
    75 -> "Heavy snow"
    80 -> "Light rain showers"
    81 -> "Moderate rain showers"
    82 -> "Heavy rain showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Unknown"
}
```

### Wind Direction Mapping
```kotlin
fun degreesToCompass(degrees: Int): String {
    val directions = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = ((degrees + 11.25) / 22.5).toInt() % 16
    return directions[index]
}

fun kmhToMph(kmh: Double): Double = kmh * 0.621371
```

## Caching Strategy

### Design
```
Shot recorded → Need weather data
    ↓
Is cache < 1 hour old?
    ├── YES → Use cached data (instant, no network)
    └── NO  → Fetch from API
                ├── Success → Update cache, use new data
                └── Failure → Save shot with null weather fields
```

### Implementation
```kotlin
class WeatherCache {
    private var cachedWeather: WeatherData? = null
    private var cacheTimestamp: Long = 0L

    fun isValid(): Boolean {
        return cachedWeather != null &&
               (System.currentTimeMillis() - cacheTimestamp) < ONE_HOUR_MS
    }

    companion object {
        const val ONE_HOUR_MS = 3_600_000L
    }
}
```

### Why In-Memory Cache?
- Simple — no extra Room table or SharedPreferences
- Weather data doesn't need to survive app restarts (we'll re-fetch on next launch)
- A golfer plays for 4 hours max — at most 4 API calls per round
- On app launch, trigger an initial fetch so the cache is warm before the first shot

## Error Handling

| Scenario | Behavior |
|----------|----------|
| No internet | Shot saves with null weather. No error shown to user. |
| API returns 500 | Same as no internet — silent fallback. |
| API timeout (> 5s) | Cancel request, save shot without weather. |
| Malformed response | Log error, save shot without weather. |
| GPS not available | Can't determine location for weather — save without. |

**Core principle:** Weather is a nice-to-have. It must NEVER block or delay shot tracking.

## Integration Points
- **App launch** → trigger initial weather fetch (warm the cache)
- **Shot saved** → attach current weather from cache
- **Analytics** → weather data enables filtering and pattern analysis
- **Shot result screen** → display temperature + condition + wind

## Retrofit Interface
```kotlin
interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): WeatherResponse
}
```

## i18n Considerations
- Weather condition labels are derived from `weatherCode` at **render time** using string resources
- Database stores the **integer code**, not the English string — this ensures language independence
- Temperature display respects locale preference (°F primary for US, °C for most other countries)
- Wind speed: mph for US/UK, km/h for metric countries

## Acceptance Criteria
- [ ] On app launch, fetch current weather from Open-Meteo using GPS coordinates
- [ ] Cache weather in-memory with a timestamp
- [ ] Cache is valid for 1 hour — no redundant API calls
- [ ] After 1 hour, next access fetches fresh data
- [ ] Temperature stored in both °F and °C
- [ ] Weather condition mapped from WMO code to readable label
- [ ] Wind speed (mph) and direction (compass label) captured
- [ ] Weather displayed on shot result screen
- [ ] Offline/failure: shot saves with null weather — no error to user
- [ ] Weather fetch never blocks shot tracking workflow
- [ ] Weather codes stored as integers for language-neutral persistence
