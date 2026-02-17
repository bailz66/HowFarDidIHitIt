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
        │ Integra-│  ← Middle: Room + DAO, API + Cache
        │  tion   │     (Instrumented or Robolectric)
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
| **GPS calibration algorithm** | Outlier rejection, median calculation, averaging. Inputs: normal cluster, cluster with spikes, all outliers, < 3 samples. |
| **Weather code mapping** | Every WMO code → correct label. Unknown code → "Unknown". |
| **Wind direction mapping** | Degrees → compass labels. Edge cases: 0° = N, 359° = N, 180° = S. |
| **Temperature conversion** | °C → °F. Known values: 0°C = 32°F, 100°C = 212°F, -40°C = -40°F. |
| **Club enum** | Sort order is correct. Display names are correct. All 18 clubs present. |
| **Weather cache** | Fresh cache (< 1hr) returns cached. Stale cache (> 1hr) returns null/triggers fetch. Empty cache returns null. |
| **Distance formatting** | Meters → yards conversion. Rounding behavior. Display string formatting. |
| **Filter logic** | Null filters (include all). Single filter. Combined filters. Empty result set. |

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
app/src/test/java/com/example/howfardidihitit/
├── location/
│   ├── HaversineTest.kt
│   ├── GpsCalibrationTest.kt
│   └── WindDirectionTest.kt
├── network/
│   ├── WeatherCodeMappingTest.kt
│   ├── TemperatureConversionTest.kt
│   └── WeatherCacheTest.kt
├── data/
│   └── ClubEnumTest.kt
└── ui/
    └── FilterLogicTest.kt
```

## Integration Tests
Test interactions between components. May require Android context (Room) or mock server (Retrofit).

### Room + DAO Tests
```kotlin
@RunWith(AndroidJUnit4::class)
class ShotDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ShotDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.shotDao()
    }

    @Test
    fun insertAndRetrieve() = runTest {
        val shot = Shot(startLat = 33.0, startLon = -84.0, ...)
        dao.insert(shot)
        val shots = dao.getAllShots().first()
        assertEquals(1, shots.size)
    }

    @Test
    fun clubStatsAggregation() = runTest {
        // Insert 3 driver shots with known distances
        // Verify AVG, MIN, MAX, COUNT
    }

    @Test
    fun filterByDateRange() = runTest {
        // Insert shots across different dates
        // Verify date filter returns correct subset
    }

    @After
    fun teardown() { db.close() }
}
```

### Weather API Tests (MockWebServer)
```kotlin
class WeatherApiTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var api: WeatherApi

    @Test
    fun `parses weather response correctly`() {
        mockServer.enqueue(MockResponse().setBody(sampleWeatherJson))
        val response = runBlocking { api.getCurrentWeather(33.0, -84.0) }
        assertEquals(22.5, response.currentWeather.temperature)
        assertEquals(1, response.currentWeather.weatherCode)
    }

    @Test
    fun `handles server error gracefully`() {
        mockServer.enqueue(MockResponse().setResponseCode(500))
        // Verify error handling
    }
}
```

### Test Location
```
app/src/androidTest/java/com/example/howfardidihitit/
├── data/
│   └── ShotDaoTest.kt
└── network/
    └── WeatherApiTest.kt
```

## UI Tests (Compose Testing)
Test user workflows through the actual Compose UI.

### Screen Tests
```kotlin
class ShotTrackerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `club selector shows all clubs`() {
        composeTestRule.setContent { ShotTrackerScreen(...) }
        composeTestRule.onNodeWithText("Driver").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lob Wedge").assertIsDisplayed()
    }

    @Test
    fun `mark start button disabled without club selection`() {
        composeTestRule.setContent { ShotTrackerScreen(...) }
        composeTestRule.onNodeWithText("Mark Start").assertIsNotEnabled()
    }

    @Test
    fun `shows calibrating indicator when marking`() {
        // Trigger mark start, verify "Calibrating..." appears
    }

    @Test
    fun `shot result displays distance and weather`() {
        // Set up state with completed shot
        // Verify distance, club, weather all displayed
    }
}
```

### Analytics Screen Tests
```kotlin
class AnalyticsScreenTest {
    @Test
    fun `shows empty state when no shots`() { ... }

    @Test
    fun `displays club stats correctly`() { ... }

    @Test
    fun `filter chips appear and are dismissible`() { ... }

    @Test
    fun `clearing filters resets to default`() { ... }
}
```

### Test Location
```
app/src/androidTest/java/com/example/howfardidihitit/
└── ui/
    ├── ShotTrackerScreenTest.kt
    ├── AnalyticsScreenTest.kt
    ├── ShotHistoryScreenTest.kt
    └── NavigationTest.kt
```

## Test Configuration

### Dependencies (libs.versions.toml)
```toml
[versions]
junit5 = "5.10.2"
mockk = "1.13.10"
turbine = "1.1.0"
mockwebserver = "4.12.0"

[libraries]
junit5-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "mockwebserver" }
```

### Test Libraries & Their Purpose
| Library | Purpose |
|---------|---------|
| JUnit 5 | Test framework — assertions, lifecycle, parameterized tests |
| MockK | Kotlin-first mocking library (mock LocationService, WeatherApi) |
| Turbine | Testing Kotlin `Flow` — assert emissions from Room queries |
| MockWebServer | Mock HTTP server for Retrofit tests |
| Compose UI Test | `createComposeRule()`, `onNodeWithText()`, etc. |
| Robolectric | Run instrumented-style tests on JVM (faster than emulator) |

## CI Integration
See [Deployment](./DEPLOYMENT.md) for the full GitHub Actions workflow. Key points:
- All tests run on every push and PR
- Unit tests run first (fast fail)
- Integration and UI tests run on an Android emulator in CI
- Test reports uploaded as artifacts
- PR cannot merge if tests fail

## Coverage Goals
| Layer | Target |
|-------|--------|
| Unit (business logic) | 90%+ |
| Integration (Room, API) | 80%+ |
| UI (critical paths) | Key workflows covered |
| Overall | 80%+ |

## Test Naming Convention
```
`description of scenario - expected result`
```
Examples:
- `calibration with 5 valid samples returns averaged coordinate`
- `cache older than 1 hour is treated as stale`
- `empty shot list shows friendly empty state`
