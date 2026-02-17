# Data Model

## Overview
All data is stored locally on-device using Room (SQLite). There is no backend or cloud sync. The database has a single table: `shots`.

## Shot Entity

```kotlin
@Entity(tableName = "shots")
data class Shot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Start position (calibrated)
    val startLat: Double,
    val startLon: Double,

    // End position (calibrated)
    val endLat: Double,
    val endLon: Double,

    // Calculated distance
    val distanceYards: Double,
    val distanceMeters: Double,

    // Club used
    val club: String,

    // Weather (nullable — may be unavailable offline)
    val temperatureF: Double? = null,
    val temperatureC: Double? = null,
    val weatherCode: Int? = null,
    val weatherCondition: String? = null,
    val windSpeedMph: Double? = null,
    val windDirectionDegrees: Int? = null,
    val windDirectionLabel: String? = null,

    // Metadata
    val timestamp: Long = System.currentTimeMillis()
)
```

## Field Descriptions

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | Auto-generated primary key |
| `startLat` | Double | No | Calibrated latitude of the start position |
| `startLon` | Double | No | Calibrated longitude of the start position |
| `endLat` | Double | No | Calibrated latitude of the end position |
| `endLon` | Double | No | Calibrated longitude of the end position |
| `distanceYards` | Double | No | Great-circle distance in yards (Haversine) |
| `distanceMeters` | Double | No | Great-circle distance in meters (Haversine) |
| `club` | String | No | Club name (e.g., "Driver", "7 Iron", "Pitching Wedge") |
| `temperatureF` | Double | Yes | Temperature in Fahrenheit at time of shot |
| `temperatureC` | Double | Yes | Temperature in Celsius at time of shot |
| `weatherCode` | Int | Yes | WMO weather code (see Weather doc) |
| `weatherCondition` | String | Yes | Human-readable weather label (e.g., "Rain", "Clear sky") |
| `windSpeedMph` | Double | Yes | Wind speed in miles per hour |
| `windDirectionDegrees` | Int | Yes | Wind direction in degrees (0-360) |
| `windDirectionLabel` | String | Yes | Compass direction (e.g., "NW", "SSE") |
| `timestamp` | Long | No | Unix epoch milliseconds when the shot was recorded |

## Club Enum

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

## DAO (Data Access Object)

```kotlin
@Dao
interface ShotDao {
    @Insert
    suspend fun insert(shot: Shot): Long

    @Query("SELECT * FROM shots ORDER BY timestamp DESC")
    fun getAllShots(): Flow<List<Shot>>

    @Query("SELECT * FROM shots WHERE club = :club ORDER BY timestamp DESC")
    fun getShotsByClub(club: String): Flow<List<Shot>>

    @Query("""
        SELECT club,
               COUNT(*) as shotCount,
               AVG(distanceYards) as avgDistance,
               MIN(distanceYards) as minDistance,
               MAX(distanceYards) as maxDistance
        FROM shots
        GROUP BY club
    """)
    fun getClubStats(): Flow<List<ClubStats>>

    @Query("SELECT * FROM shots WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getShotsByDateRange(startTime: Long, endTime: Long): Flow<List<Shot>>

    @Query("DELETE FROM shots WHERE id = :shotId")
    suspend fun deleteShot(shotId: Long)
}
```

## Analytics Data Classes

```kotlin
data class ClubStats(
    val club: String,
    val shotCount: Int,
    val avgDistance: Double,
    val minDistance: Double,
    val maxDistance: Double
)
```

## Database Migrations
For v1, no migrations needed. For future versions:
- Use Room's `@Database(version = N)` with `Migration` objects
- Never lose user data — always provide migration paths
- Test migrations with `MigrationTestHelper`

## Indexing
- Primary key on `id` (automatic)
- Consider index on `club` if analytics queries become slow (unlikely with < 10k rows)
- Consider index on `timestamp` for date range queries

## String Storage for i18n
The `club` field stores the **enum name** (e.g., `"DRIVER"`, `"IRON_7"`), not the display string. This ensures the database is language-independent. The `displayName` is resolved at render time via the enum or a string resource lookup.

Similarly, `weatherCondition` stores the **English label** as a fallback, but the UI can map `weatherCode` to a localized string resource at render time.
