# Feature 1: Shot Tracking (GPS)
> GitHub Issue: #2

## Purpose
The core feature of the app. A golfer hits a shot, wants to know how far it went, and wants to record what club they used. The entire UX should feel like: **tap, walk, tap, done.** Minimal friction.

## User Workflow

```
[Open App]
    ↓
[Select Club] ← Scrollable list: Driver → Lob Wedge
    ↓
[Tap "Mark Start"]
    ↓
[Calibrating... (1.5s)] ← GPS samples collected, averaged, outliers rejected
    ↓
[Start Pinned ✓]
    ↓
[Walk/drive to ball]
    ↓                   ← Live distance updates on screen (yards + meters)
[Arrive at ball]
    ↓
[Tap "Mark End"]
    ↓
[Calibrating... (1.5s)] ← Same GPS calibration for end position
    ↓
[End Pinned ✓]
    ↓
[Shot Result Display]
    ├── Club: Driver
    ├── Distance: 245 yards (224m)
    ├── Weather: 72°F, Clear sky, Wind 8mph NW
    └── [Save ✓ — automatic]
    ↓
[Ready for next shot]
```

## GPS Calibration Algorithm

### Why Calibrate?
GPS on phones is accurate to ~3-5 meters under open sky, which is fine for golf distances. But a single GPS fix can occasionally spike 10-20m off due to atmospheric interference, multipath reflection, or satellite geometry. The calibration algorithm smooths out this noise.

### Algorithm
1. **Trigger**: User taps "Mark Start" or "Mark End"
2. **Collection**: Request GPS fixes at the highest available rate for **~1.5 seconds**
   - Target: 5-10 readings (depends on device GPS update speed)
   - Use `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`
3. **Outlier Rejection**:
   - Calculate the **median** latitude and longitude from all samples
   - Compute the distance from each sample to the median
   - **Discard** any sample more than **10 meters** from the median (these are GPS spikes)
4. **Averaging**: Take the **arithmetic mean** of remaining valid samples
5. **Result**: Single calibrated `(latitude, longitude)` coordinate
6. **Fallback**: If fewer than 3 valid readings after rejection, use the best available reading but show a "Low accuracy" warning

### Why Median + Mean?
- **Median** is robust to outliers — it finds the "center" of the cluster
- **Mean** of the cleaned set gives the best positional estimate
- This two-step approach handles the common case (tight cluster + 1-2 spikes) very well

### Pseudocode
```kotlin
fun calibrate(samples: List<GpsCoordinate>): CalibratedResult {
    if (samples.size < 3) return CalibratedResult(samples.best(), lowAccuracy = true)

    val medianLat = samples.map { it.lat }.median()
    val medianLon = samples.map { it.lon }.median()
    val median = GpsCoordinate(medianLat, medianLon)

    val valid = samples.filter { haversine(it, median) <= 10.0 } // 10m threshold

    if (valid.size < 3) return CalibratedResult(median, lowAccuracy = true)

    val calibrated = GpsCoordinate(
        lat = valid.map { it.lat }.average(),
        lon = valid.map { it.lon }.average()
    )
    return CalibratedResult(calibrated, lowAccuracy = false)
}
```

## Live Distance Tracker
- Activates after the start pin is set
- Uses standard location updates (every 1-2 seconds, `PRIORITY_HIGH_ACCURACY`)
- Calculates Haversine distance from start pin to current location
- Displays: `"142 yards (130m)"` — updates in real-time
- Useful for: finding the ball, getting a sense of distance while walking

## Distance Calculation: Haversine Formula
The Haversine formula calculates great-circle distance between two points on a sphere.

```kotlin
fun haversineMeters(start: GpsCoordinate, end: GpsCoordinate): Double {
    val R = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(end.lat - start.lat)
    val dLon = Math.toRadians(end.lon - start.lon)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(start.lat)) *
            cos(Math.toRadians(end.lat)) *
            sin(dLon / 2).pow(2)
    val c = 2 * asin(sqrt(a))
    return R * c
}

fun metersToYards(meters: Double): Double = meters * 1.09361
```

At golf distances (50-400 yards), this is accurate to within centimeters — far more precise than GPS itself.

## Club Selection
- Presented **before** "Mark Start" so the golfer sets context while standing at the ball
- Scrollable list or horizontal chip selector grouped by category
- Remember last selected club for convenience (most golfers hit the same club multiple times in a row at the range)
- Club list: see [Data Model — Club Enum](./DATA_MODEL.md)

## Edge Cases

| Scenario | Handling |
|----------|----------|
| GPS permissions denied | Show explanation dialog → system permission request → block shot tracking until granted |
| GPS accuracy > 20m | Show warning banner: "Low GPS signal — results may be less accurate" |
| User cancels mid-shot | "Reset" button clears start pin, returns to club selection |
| App backgrounded while walking | Resume location tracking when app returns to foreground |
| Very short shot (< 5 yards) | Record normally — could be a chip |
| Very long shot (> 400 yards) | Record normally — par 5 drive + roll |
| No club selected | Prompt user to select a club before allowing "Mark Start" |
| GPS fix takes too long | Timeout after 5 seconds, use whatever samples were collected |

## Dependencies
- `com.google.android.gms:play-services-location` — FusedLocationProviderClient
- `androidx.room:room-runtime` + `room-ktx` — local persistence
- Kotlin Coroutines — async GPS collection

## Acceptance Criteria
- [ ] User can select a club from the full list (Driver → Lob Wedge) before starting a shot
- [ ] Tapping "Mark Start" collects GPS samples over ~1.5s and calibrates to a single coordinate
- [ ] A "Calibrating..." indicator is visible during GPS sampling
- [ ] After start pin is set, live distance (yards + meters) updates on screen as user moves
- [ ] Tapping "Mark End" performs the same GPS calibration for the end position
- [ ] Shot distance is calculated via Haversine formula, displayed in yards and meters
- [ ] Shot is automatically saved to Room DB with all fields
- [ ] User can reset/cancel a shot in progress
- [ ] GPS permission handling with explanation dialog
- [ ] Low GPS accuracy warning when device-reported accuracy exceeds 20m
- [ ] App resumes tracking correctly if backgrounded and reopened mid-shot
