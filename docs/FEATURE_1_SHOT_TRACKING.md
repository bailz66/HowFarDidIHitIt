# Feature 1: Shot Tracking (GPS)
> GitHub Issue: #2

## Purpose
The core feature of the app. A golfer hits a shot, wants to know how far it went, and wants to record what club they used. The entire UX should feel like: **tap, walk, tap, done.** Minimal friction.

## User Workflow

```
[Open App]
    ↓
[Grant Location + Notification Permission] ← Requested once at launch
    ↓
[Select Club] ← Scrollable chips: Driver → Lob Wedge
    ↓
[Tap "SMACK"]
    ↓
[Foreground service starts] ← Notification: "Tracking your shot..."
[Screen keep-awake enabled]
[15-min shot timeout begins]
    ↓
[Calibrating... (3.5s)] ← GPS samples collected, weighted, outliers rejected
    ↓
[Start Pinned ✓]
    ↓
[Walk/drive to ball] ← GPS alive even with screen locked
    ↓               ← Live distance updates on screen (yards + meters)
[Arrive at ball]
    ↓
[Tap "TRACK"]
    ↓
[Calibrating... (2s)] ← GPS calibration for end position
    ↓                  ← Weather fetched in parallel via async
[End Pinned ✓]
    ↓
[Shot Result Display]
    ├── Club: Driver
    ├── Distance: 245 yards (224m)
    ├── GPS Accuracy: ±3 yd
    ├── Weather: 72°F, Clear sky, Wind 8mph NW
    ├── Wind-adjusted carry: +5 yards (tailwind)
    └── [Save ✓ — persisted to SharedPreferences + Firestore]
    ↓
[Foreground service stops, screen keep-awake cleared, timeout cancelled]
    ↓
[Ready for next shot]

TIMEOUT (15 min, no TRACK):
    → Toast: "Shot timed out after 15 minutes"
    → Auto-reset to club selection
```

## GPS Calibration Algorithm

### Why Calibrate?
GPS on phones is accurate to ~3-5 meters under open sky, which is fine for golf distances. But a single GPS fix can occasionally spike 10-20m off due to atmospheric interference, multipath reflection, or satellite geometry. The calibration algorithm smooths out this noise.

### Algorithm (Inverse-Variance Weighted)
1. **Trigger**: User taps "SMACK" or "TRACK"
2. **Collection**: GPS fixes collected at 500ms intervals — **3.5 seconds** for start, **2 seconds** for end — via `LocationProvider`
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

## Foreground Service

A lightweight foreground service (`ShotTrackingService`) keeps the app's process priority elevated during tracking, ensuring GPS updates continue when the screen is locked. The service is a **notification-only shell** — it does not own GPS collection.

- **Start**: When user taps "SMACK" after GPS calibration succeeds
- **Stop**: When user taps "TRACK", resets the shot, or the 15-minute timeout fires
- **Notification**: "Tracking your shot..." — tap to bring the app to front; IMPORTANCE_LOW (silent)
- **No `ACCESS_BACKGROUND_LOCATION`** needed — foreground service counts as "foreground" access
- **No WakeLock** — `FLAG_KEEP_SCREEN_ON` on the Activity window is simpler and auto-releases

## Shot Timeout

A 15-minute coroutine timer starts when the user taps "SMACK". If "TRACK" is not tapped within that window:
- Toast: "Shot timed out after 15 minutes"
- Auto-reset to club selection (calls `nextShot()`)
- Foreground service stopped, screen keep-awake cleared

## Distance Validation

After Haversine calculation, distances are validated:
- **NaN or infinite** → distance set to 0, toast warning shown
- **> 500 yards** → capped at 500, toast: "GPS reading seems off — distance capped at 500 yards"

## Edge Cases

| Scenario | Handling |
|----------|----------|
| GPS permissions denied | Permission requested once at launch; tracking disabled until granted |
| GPS accuracy > 20m | Samples rejected by accuracy gate; calibration may fall back to raw position |
| GPS accuracy < 0.1m | Samples rejected (suspiciously precise, likely spoofed) |
| User cancels mid-shot | "Reset" button clears start pin, stops service, returns to club selection |
| Screen locked while walking | Foreground service keeps GPS alive; screen unlocks to live distance |
| App backgrounded while walking | Foreground service prevents process kill; GPS continues |
| Very short shot (< 5 yards) | Record normally — could be a chip |
| Very long shot (> 500 yards) | Capped at 500 with warning toast |
| NaN/infinite distance | Set to 0 with warning toast |
| No club selected | "SMACK" requires club selection |
| Shot abandoned (15 min) | Auto-reset with toast, service stopped |
| GPS fix takes too long | Calibration window collects whatever samples are available |

## Dependencies
- `com.google.android.gms:play-services-location` — FusedLocationProviderClient
- Kotlin Coroutines — async GPS collection and parallel weather fetch

## Acceptance Criteria
- [x] User can select a club from the full list (Driver → Lob Wedge) before starting a shot
- [x] Tapping "SMACK" collects GPS samples over ~3.5s and calibrates to a single coordinate
- [x] A "Calibrating..." indicator is visible during GPS sampling
- [x] After start pin is set, live distance (yards + meters) updates on screen as user moves
- [x] Tapping "TRACK" performs GPS calibration (2s) for the end position
- [x] Shot distance is calculated via Haversine formula, displayed in yards and meters
- [x] Shot is automatically persisted (SharedPreferences + Firestore if signed in)
- [x] User can reset/cancel a shot in progress
- [x] GPS permission requested at launch
- [x] Calibration uses inverse-variance weighting with MAD outlier rejection
- [x] Foreground service keeps GPS alive when screen locks
- [x] Screen stays on during active tracking phases (FLAG_KEEP_SCREEN_ON)
- [x] 15-minute shot timeout auto-resets abandoned shots
- [x] GPS accuracy displayed on result screen (warns if >15m)
- [x] Distance clamped: NaN→0, >500yd→500 with warning toasts
