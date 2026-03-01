# Feature 1: Shot Tracking (GPS)
> GitHub Issue: #2

## Purpose
The core feature of the app. A golfer hits a shot, wants to know how far it went, and wants to record what club they used. The entire UX should feel like: **tap, walk, tap, done.** Minimal friction.

## User Workflow

```
[Open App]
    ↓
[Grant Location Permission] ← Requested once at launch via MainActivity
    ↓
[Select Club] ← Scrollable list: Driver → Lob Wedge
    ↓
[Tap "Mark Start"]
    ↓
[Calibrating... (2.5s)] ← GPS samples collected, weighted, outliers rejected
    ↓
[Start Pinned ✓]
    ↓
[Walk/drive to ball]
    ↓                   ← Live distance updates on screen (yards + meters)
[Arrive at ball]
    ↓
[Tap "Mark End"]
    ↓
[Calibrating... (2.5s)] ← Same GPS calibration for end position
    ↓                   ← Weather fetched in parallel via async
[End Pinned ✓]
    ↓
[Shot Result Display]
    ├── Club: Driver
    ├── Distance: 245 yards (224m)
    ├── Weather: 72°F, Clear sky, Wind 8mph NW
    └── [Save ✓ — automatic to in-memory history]
    ↓
[Ready for next shot]
```

## GPS Calibration Algorithm

### Why Calibrate?
GPS on phones is accurate to ~3-5 meters under open sky, which is fine for golf distances. But a single GPS fix can occasionally spike 10-20m off due to atmospheric interference, multipath reflection, or satellite geometry. The calibration algorithm smooths out this noise.

### Algorithm (Inverse-Variance Weighted)
1. **Trigger**: User taps "Mark Start" or "Mark End"
2. **Collection**: GPS fixes collected at 500ms intervals for **2.5 seconds** via `LocationProvider`
   - Uses `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`
   - Each sample includes latitude, longitude, and reported accuracy in meters
3. **Cold-Start Rejection**: First GPS sample is discarded (often inaccurate due to GPS cold-start jitter)
4. **Accuracy Gating**: Samples with reported accuracy > 20m are rejected
5. **Weighted Centroid**: Compute initial position using inverse-variance weights (`w = 1/accuracy²`)
   - More accurate samples receive exponentially higher weight
6. **MAD Outlier Rejection**: Calculate distances from weighted centroid, compute median absolute deviation (MAD), reject samples beyond MAD × 2.5
7. **Final Weighted Average**: Recompute from inliers only
8. **Accuracy Estimate**: Weighted RMS distance from final position
9. **Fallback**: If fewer than 3 valid readings after rejection, fall back to raw GPS position

### Why Inverse-Variance Weighting?
- GPS reports an accuracy estimate with each fix — this is valuable signal
- A 3m-accuracy sample should count much more than a 15m-accuracy sample
- Inverse-variance (`1/accuracy²`) is the statistically optimal weighting for Gaussian noise
- Combined with MAD outlier rejection, this handles both systematic bias and random spikes

### Implementation
```kotlin
fun calibrateWeighted(samples: List<GpsSample>): CalibratedPosition? {
    if (samples.size < 2) return null
    val withoutFirst = samples.drop(1)                    // Cold-start rejection
    val gated = withoutFirst.filter {                     // Accuracy gate
        it.accuracyMeters in 0.1..ACCURACY_GATE_METERS
    }
    if (gated.size < MIN_SAMPLES) return null             // Need ≥ 3 valid samples
    val centroid = weightedCentroid(gated)                 // Inverse-variance weighted
    val distances = gated.map { haversineMeters(it, centroid) }
    val madThreshold = median(distances) * OUTLIER_MAD_FACTOR  // MAD × 2.5
    val inliers = gated.filterIndexed { i, _ -> distances[i] <= madThreshold }
    if (inliers.size < MIN_SAMPLES) return null
    return CalibratedPosition(weightedCentroid(inliers), estimatedAccuracy, inliers.size)
}
```

## Live Distance Tracker
- Activates after the start pin is set
- Polls current GPS position every 1 second
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
- Horizontal scrolling chips grouped by category (Woods, Hybrids, Irons, Wedges)
- Clubs can be enabled/disabled in Settings to match your bag
- Default selection: Driver (most common first shot)

## Edge Cases

| Scenario | Handling |
|----------|----------|
| GPS permissions denied | Permission requested once at launch; tracking disabled until granted |
| GPS accuracy > 20m | Samples rejected by accuracy gate; calibration may fall back to raw position |
| User cancels mid-shot | "Reset" button clears start pin, returns to club selection |
| App backgrounded while walking | Location updates continue via LocationProvider Flow |
| Very short shot (< 5 yards) | Record normally — could be a chip |
| Very long shot (> 400 yards) | Record normally — par 5 drive + roll |
| No club selected | "Mark Start" requires club selection |
| GPS fix takes too long | 2.5s calibration window collects whatever samples are available |

## Dependencies
- `com.google.android.gms:play-services-location` — FusedLocationProviderClient
- Kotlin Coroutines — async GPS collection and parallel weather fetch

## Acceptance Criteria
- [x] User can select a club from the full list (Driver → Lob Wedge) before starting a shot
- [x] Tapping "Mark Start" collects GPS samples over ~2.5s and calibrates to a single coordinate
- [x] A "Calibrating..." indicator is visible during GPS sampling
- [x] After start pin is set, live distance (yards + meters) updates on screen as user moves
- [x] Tapping "Mark End" performs the same GPS calibration for the end position
- [x] Shot distance is calculated via Haversine formula, displayed in yards and meters
- [x] Shot is automatically saved to in-memory history
- [x] User can reset/cancel a shot in progress
- [x] GPS permission requested at launch
- [x] Calibration uses inverse-variance weighting with MAD outlier rejection
