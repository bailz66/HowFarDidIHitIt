# Testing Strategy
> GitHub Issues: #6, #7, #8, #9

## Overview
A layered testing approach covering unit tests, integration tests, and UI tests. Tests should be fast, reliable, and run in CI on every push.

## Test Pyramid

```
        ┌─────────┐
        │  UI/E2E │  ← Fewest, slowest, highest confidence
        │  Tests  │     (Compose UI tests, Espresso)
        ├─────────┤
        │ Integra-│  ← Middle: instrumented tests
        │  tion   │     (Android context required)
        ├─────────┤
        │  Unit   │  ← Most, fastest, foundation
        │  Tests  │     (JUnit 5, pure Kotlin)
        └─────────┘
```

## Unit Tests (JUnit 5)
Fast, pure Kotlin tests. No Android framework dependencies. Run on JVM.

### What to Unit Test

| Component | Tests |
|-----------|-------|
| **Haversine formula** | Known coordinate pairs → expected distances. Edge cases: same point (0m), antipodal points, short distances, long distances. |
| **GPS calibration algorithm** | Outlier rejection, weighted centroid, accuracy gating. Inputs: normal cluster, cluster with spikes, all outliers, < 3 samples. |
| **Weather code mapping** | Every WMO code → correct label. Unknown code → "Unknown". |
| **Wind direction mapping** | Degrees → compass labels. Edge cases: 0° = N, 359° = N, 180° = S. |
| **Temperature conversion** | °C → °F. Known values: 0°C = 32°F, 100°C = 212°F, -40°C = -40°F. |
| **Club enum** | Sort order is correct. Display names are correct. All 18 clubs present. |
| **Weather API parsing** | Valid JSON → WeatherData. Missing fields → null. Malformed JSON → null. |
| **Distance formatting** | Meters → yards conversion. Rounding behavior. Display string formatting. |
| **Data validation** | GPS coordinates, shot distances, weather values, timestamps. |

### Example
```kotlin
class HaversineTest {
    @Test
    fun `distance between same point is zero`() {
        val point = GpsCoordinate(33.749, -84.388)
        assertEquals(0.0, haversineMeters(point, point), 0.001)
    }

    @Test
    fun `distance between known points is accurate`() {
        // Atlanta to Augusta (~230km)
        val atlanta = GpsCoordinate(33.749, -84.388)
        val augusta = GpsCoordinate(33.474, -82.010)
        val distance = haversineMeters(atlanta, augusta)
        assertEquals(230_000.0, distance, 1000.0) // ±1km tolerance
    }
}
```

### Test Location
```
app/src/test/java/com/smacktrack/golf/
├── data/
│   ├── ClubEnumBoundaryTest.kt
│   └── DistanceFormattingBoundaryTest.kt
├── location/
│   ├── GpsCalibrationBoundaryTest.kt
│   └── HaversineBoundaryTest.kt
├── network/
│   ├── WeatherBoundaryTest.kt
│   └── WeatherServiceParseTest.kt
└── validation/
    ├── GpsCoordinateValidationTest.kt
    ├── ShotDistanceValidationTest.kt
    ├── ShotEntityValidationTest.kt
    ├── TimestampValidationTest.kt
    └── WeatherDataValidationTest.kt
```

## Integration Tests
Test interactions between components. Require Android context.

### Instrumented Tests
```
app/src/androidTest/java/com/smacktrack/golf/
└── ExampleInstrumentedTest.kt
```

Currently a basic instrumented test to verify the app context. Room/DAO tests are not applicable — the app uses in-memory data only (no database).

## UI Tests (Compose Testing)
Test user workflows through the actual Compose UI. Currently planned but not yet implemented.

## Test Configuration

### Dependencies (libs.versions.toml)
```toml
[versions]
junit5 = "5.10.2"
androidJunit5 = "1.10.2.0"

[libraries]
junit5-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit5" }
junit5-launcher = { group = "org.junit.platform", name = "junit-platform-launcher", version = "1.10.2" }
json = { group = "org.json", name = "json", version = "20231013" }
```

### Test Libraries & Their Purpose
| Library | Purpose |
|---------|---------|
| JUnit 5 | Test framework — assertions, lifecycle, parameterized tests |
| JUnit 5 Params | `@ParameterizedTest` with `@CsvSource`, `@EnumSource`, `@ValueSource` |
| JUnit Platform Launcher | Required runtime dependency for JUnit 5 on Android/Gradle |
| org.json | JVM-compatible JSON parsing for `WeatherServiceParseTest` (mirrors Android's `org.json`) |
| Compose UI Test | `createComposeRule()`, `onNodeWithText()`, etc. (planned) |

## CI Integration
See [Deployment](./DEPLOYMENT.md) for the full GitHub Actions workflow. Key points:
- All tests run on every push and PR to `master`, `main`, and `release/**`
- Unit tests run first (fast fail)
- Integration tests run on an Android emulator in CI (after unit tests pass)
- Test reports uploaded as artifacts
- PR cannot merge if tests fail

## Coverage Goals
| Layer | Target |
|-------|--------|
| Unit (business logic) | 90%+ |
| Integration | Key paths covered |
| UI (critical paths) | Key workflows covered |
| Overall | 80%+ |

## Test Naming Convention
```
`description of scenario - expected result`
```
Examples:
- `calibration with 5 valid samples returns averaged coordinate`
- `parses valid Open-Meteo response`
- `empty shot list shows friendly empty state`

---

## Boundary Value Testing
> GitHub Issue: #15

All boundary tests use JUnit 5 `@ParameterizedTest` with `@CsvSource` for tabular, data-driven test cases. This ensures systematic coverage of edge cases and boundary conditions.

### Haversine Distance Boundaries (`HaversineBoundaryTest.kt`)

| Test Case | Start | End | Expected | Tolerance |
|-----------|-------|-----|----------|-----------|
| Same point | (33.749, -84.388) | (33.749, -84.388) | 0m | 0.1m |
| Short chip (~5yds) | (33.749, -84.388) | (33.74904, -84.388) | ~4.4m | 1.0m |
| Typical PW (~108yds) | (33.749, -84.388) | (33.7499, -84.388) | ~100m | 5.0m |
| Long driver (~300yds) | (33.749, -84.388) | (33.7515, -84.388) | ~278m | 10.0m |
| North Pole to 89N | (90, 0) | (89, 0) | ~111km | 100m |
| South Pole to 89S | (-90, 0) | (-89, 0) | ~111km | 100m |
| Equator 1° lon | (0, 0) | (0, 1) | ~111km | 100m |
| Date Line crossing | (0, 179.5) | (0, -179.5) | ~111km | 200m |
| Antipodal points | (0, 0) | (0, 180) | ~20,015km | 1km |
| Negative latitude | (-33.868, 151.207) | (-33.856, 151.215) | ~1.5km | 100m |
| Prime Meridian crossing | (51.5, -0.1) | (51.5, 0.1) | ~13.8km | 200m |
| Atlanta to Augusta | (33.749, -84.388) | (33.474, -82.010) | ~230km | 1.5km |

### GPS Calibration Boundaries (`GpsCalibrationBoundaryTest.kt`)

| Test Case | Input | Expected |
|-----------|-------|----------|
| Min samples (3) | 3 tight points | Valid calibrated coordinate |
| Below min (2) | 2 points | `null` |
| Single sample | 1 point | `null` |
| Empty list | 0 points | `null` |
| Tight cluster + 1 spike | 4 close + 1 far | Close to cluster center |
| Tight cluster + 2 spikes | 3 close + 2 far | Close to cluster center |
| All identical | 5x same point | That exact coordinate |
| Extreme coordinates | Near poles | Valid result |
| Crossing 0° longitude | Points around Greenwich | lon ≈ 0 |
| 10 samples (max typical) | 10 spread points | Close to center |
| All outliers (scattered) | 3 far-apart points | `null` or valid |
| Negative latitude | Southern hemisphere | Correct negative lat |

### Weather Mapping Boundaries (`WeatherBoundaryTest.kt`)

**Temperature Conversion:**

| Celsius | Fahrenheit | Notes |
|---------|-----------|-------|
| -273.15 | -459.67 | Absolute zero |
| 0.0 | 32.0 | Freezing point |
| 100.0 | 212.0 | Boiling point |
| -40.0 | -40.0 | Crossover point |
| 37.0 | 98.6 | Body temperature |
| -89.2 | -128.56 | Coldest on Earth |
| 56.7 | 134.06 | Hottest on Earth |

**Wind Direction:**

| Degrees | Expected | Boundary |
|---------|----------|----------|
| 0 | N | Start of N sector |
| 22 | N | End of N sector |
| 23 | NE | Start of NE sector |
| 337 | NW | End of NW sector |
| 338 | N | Wrap-around to N |
| 359 | N | Last valid degree |
| 360 | N | Normalized to 0 |
| -1 | ERROR | Below minimum |
| 361 | ERROR | Above maximum |

**WMO Weather Codes:**

| Code | Expected Label |
|------|---------------|
| 0 | Clear sky |
| 99 | Thunderstorm with heavy hail |
| -1 | Unknown |
| 100 | Unknown |
| MAX_INT | Unknown |

### WeatherService JSON Parsing (`WeatherServiceParseTest.kt`)

| Test Case | Input | Expected |
|-----------|-------|----------|
| Valid response | Complete `current` object | All 4 fields parsed |
| Zero values | All fields = 0 | Valid WeatherData with zeros |
| Negative temp | -12.7°C | Parsed correctly |
| Empty JSON | `""` | `null` |
| Invalid JSON | `"not json"` | `null` |
| Missing `current` | `{ "hourly": {} }` | `null` |
| Missing fields | Only `temperature_2m` | `null` |
| Extra fields | Extra fields in `current` | Parsed (extra ignored) |

### Club Enum Boundaries (`ClubEnumBoundaryTest.kt`)

| Test Case | Expected |
|-----------|----------|
| Total count | 18 clubs |
| First sort order | 1 (Driver) |
| Last sort order | 18 (4 Hybrid) |
| Consecutive sort orders | 1, 2, 3, ..., 18 |
| No duplicate sort orders | All unique |
| No duplicate display names | All unique |
| All categories represented | WOOD, IRON, WEDGE, PUTTER, HYBRID |
| Driver properties | Wood category, sort order 1 |

### Distance Formatting Boundaries (`DistanceFormattingBoundaryTest.kt`)

| Meters | Expected Yards | Notes |
|--------|---------------|-------|
| 0.0 | 0.0 | Zero distance |
| 0.9144 | 1.0 | Exactly 1 yard |
| 91.44 | 100.0 | 100 yards |
| 274.32 | 300.0 | Long driver |
| Negative | Negative | Passthrough |
| NaN | NaN | Passthrough |
| Infinity | Infinity | Passthrough |

---

## Data Validation Testing
> GitHub Issue: #16

Validation tests ensure all data entering the system is physically plausible and internally consistent.

### GPS Coordinate Validation (`GpsCoordinateValidationTest.kt`)

| Field | Valid Range | Invalid Examples |
|-------|-----------|-----------------|
| Latitude | [-90, 90] | 90.1, -90.1, NaN, Infinity |
| Longitude | [-180, 180] | 180.1, -180.1, NaN, -Infinity |

### Shot Distance Validation (`ShotDistanceValidationTest.kt`)

**Hard Limits (all clubs):** 0-500 yards

**Per-Club Plausible Ranges:**

| Club | Min (yds) | Max (yds) |
|------|-----------|-----------|
| Driver | 100 | 400 |
| 3 Wood | 80 | 280 |
| 5 Wood | 70 | 250 |
| 7 Wood | 60 | 230 |
| 3 Iron | 60 | 230 |
| 4 Iron | 55 | 220 |
| 5 Iron | 50 | 210 |
| 6 Iron | 45 | 200 |
| 7 Iron | 40 | 190 |
| 8 Iron | 35 | 180 |
| 9 Iron | 30 | 170 |
| Pitching Wedge | 20 | 160 |
| Gap Wedge | 15 | 150 |
| Sand Wedge | 10 | 130 |
| Lob Wedge | 5 | 120 |
| Putter | 1 | 100 |
| 3 Hybrid | 60 | 240 |
| 4 Hybrid | 55 | 230 |

### Weather Data Validation (`WeatherDataValidationTest.kt`)

| Field | Valid Range | Source |
|-------|-----------|--------|
| Temperature | -89.2°C to 56.7°C | Earth record extremes |
| Wind speed | 0-253 mph | Record wind speed |
| Wind direction | 0-359° | Compass degrees |
| All-or-nothing | All 4 fields present, or all null | Data consistency |

### Timestamp Validation (`TimestampValidationTest.kt`)

| Rule | Details |
|------|---------|
| Positive | Must be > 0 |
| After 2023 | Must be >= 1672531200000 (Jan 1 2023 UTC) |
| Not future | Must be <= now + 60s tolerance |
| Milliseconds | Must be >= 1,000,000,000,000 (rejects seconds) |

### Shot Entity Validation (`ShotEntityValidationTest.kt`)

Combines all validators for full entity integrity:
- Valid complete shot with weather
- Valid shot without weather (all weather fields null)
- Invalid distance rejected even with valid coordinates
- Invalid coordinates rejected
- Implausible club/distance combinations detected
- Partial weather data rejected
- All club ranges within hard distance limits

---

## Parameterized Test Examples (JUnit 5)

### `@ParameterizedTest` with `@CsvSource`
```kotlin
@ParameterizedTest(name = "{0}°C = {1}°F")
@CsvSource(
    "0.0,    32.0,  0.01",
    "100.0, 212.0,  0.01",
    "-40.0,  -40.0, 0.01"
)
fun `celsius to fahrenheit`(celsius: Double, expectedF: Double, tolerance: Double) {
    assertEquals(expectedF, celsiusToFahrenheit(celsius), tolerance)
}
```

### `@ParameterizedTest` with `@EnumSource`
```kotlin
@ParameterizedTest
@EnumSource(Club::class)
fun `every club has a plausible range`(club: Club) {
    assertNotNull(CLUB_DISTANCE_RANGES[club])
}
```

### `@ParameterizedTest` with `@ValueSource`
```kotlin
@ParameterizedTest
@ValueSource(doubles = [Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY])
fun `special floating point values are rejected`(value: Double) {
    assertFalse(validateDistance(value).isValid)
}
```

---

## Test File Inventory

### Boundary Value Tests (`app/src/test/`)
```
com/smacktrack/golf/
├── data/
│   ├── ClubEnumBoundaryTest.kt         (8 test cases)
│   └── DistanceFormattingBoundaryTest.kt (8 parameterized groups)
├── location/
│   ├── HaversineBoundaryTest.kt        (12 parameterized cases)
│   └── GpsCalibrationBoundaryTest.kt   (12 test cases)
└── network/
    ├── WeatherBoundaryTest.kt          (25+ parameterized cases)
    └── WeatherServiceParseTest.kt      (8 test cases)
```

### Data Validation Tests (`app/src/test/`)
```
com/smacktrack/golf/
└── validation/
    ├── GpsCoordinateValidationTest.kt   (coordinate range + NaN/Infinity)
    ├── ShotDistanceValidationTest.kt    (hard limits + per-club ranges)
    ├── WeatherDataValidationTest.kt     (Earth extremes + all-or-nothing)
    ├── TimestampValidationTest.kt       (epoch ms, not future, after 2023)
    └── ShotEntityValidationTest.kt      (full entity integration validation)
```

**Total: 11 test files** across boundary, validation, and API parsing categories.
