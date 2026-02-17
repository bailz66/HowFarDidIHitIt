# Feature 3: Shot Analytics
> GitHub Issue: #4

## Purpose
The whole point of tracking shots is to **learn your distances**. When someone asks "how far do you hit your 7-iron?" you should pull up this screen and give a real, data-backed answer instead of guessing.

## Screens

### Club Analytics (Primary View)
A list of all clubs the user has recorded shots with, showing aggregated stats.

**Per-club card:**
| Stat | Description | Example |
|------|-------------|---------|
| Club name | Display name | "7 Iron" |
| Average distance | Mean of all shots | "152 yds" |
| Longest shot | Max distance | "165 yds" |
| Shortest shot | Min distance | "138 yds" |
| Shot count | Total recorded | "23 shots" |
| Avg temperature | Mean temp across shots | "68°F" |

**Ordering:** Natural club order — Driver at top → Lob Wedge at bottom (via `Club.sortOrder`).

**Empty state:** "Hit some shots to see your stats here!" with a button to navigate to the shot tracker.

### Shot History (Secondary View)
A chronological list of every recorded shot.

**Per-shot row:**
| Field | Example |
|-------|---------|
| Date/time | "Feb 17, 2026 — 2:30 PM" |
| Club | "Driver" |
| Distance | "245 yds (224m)" |
| Temperature | "72°F" |
| Weather | "Clear sky" |
| Wind | "12 mph NW" |

**Default sort:** Most recent first.

## Filtering System

### Available Filters
| Filter | Type | Details |
|--------|------|---------|
| **Club** | Multi-select chips | Select one or more clubs. Default: all. |
| **Date range** | Presets + custom picker | "Last 7 days", "Last 30 days", "Last 90 days", "All time" (default). Custom from/to date picker. |
| **Weather condition** | Multi-select chips | Filter by weather: Clear, Cloudy, Rain, Fog, Snow, etc. |
| **Temperature range** | Range slider | Min/max temperature (°F). Example: 50°F – 80°F. |

### Filter UI
- **Filter bar** at the top of both views (shared state)
- Active filters shown as **dismissible chips**
- "Clear all" button when any filter is active
- Tapping a chip opens its filter selector

### Filter Behavior
- Filters apply to **both** Club Analytics and Shot History simultaneously
- Analytics recalculate from only the filtered shots
- Empty filter results show: "No shots match your filters" + "Clear filters" button
- Filters reset on app restart (not persisted)
- Null weather fields: shots without weather data are excluded from weather/temperature filters (but included in club and date filters)

### Filter Use Cases
| Question | Filters Applied |
|----------|----------------|
| "How far am I hitting my driver this month?" | Club: Driver, Date: Last 30 days |
| "Do I hit shorter in cold weather?" | Temperature: < 50°F |
| "How do I perform in the rain?" | Weather: Rain |
| "What were my distances last summer?" | Date: Jun 1 – Aug 31 |
| "Show me all my wedge shots" | Club: PW, GW, SW, LW |

### Room Queries for Filters
```kotlin
@Query("""
    SELECT * FROM shots
    WHERE (:clubs IS NULL OR club IN (:clubs))
    AND (:startTime IS NULL OR timestamp >= :startTime)
    AND (:endTime IS NULL OR timestamp <= :endTime)
    AND (:weatherConditions IS NULL OR weatherCondition IN (:weatherConditions))
    AND (:minTemp IS NULL OR temperatureF >= :minTemp)
    AND (:maxTemp IS NULL OR temperatureF <= :maxTemp)
    ORDER BY timestamp DESC
""")
fun getFilteredShots(
    clubs: List<String>?,
    startTime: Long?,
    endTime: Long?,
    weatherConditions: List<String>?,
    minTemp: Double?,
    maxTemp: Double?
): Flow<List<Shot>>
```

## Navigation
- **Bottom navigation** with 3 tabs: Shot Tracker | Analytics | History
- Analytics and History tabs share the filter state
- Navigating between tabs preserves filter selections

## Data Source
- All data from local Room database
- Room `Flow<T>` queries — UI updates reactively when new shots are added
- No network calls — entirely offline

## Edge Cases

| Scenario | Handling |
|----------|----------|
| No shots recorded | Friendly empty state with CTA to shot tracker |
| No shots match filters | "No shots match your filters" + clear button |
| Only 1 shot with a club | Show stats normally (avg = that one shot) |
| Null weather on some shots | Exclude from weather/temp filters and averages, include in club/date |
| Very many shots (1000+) | Room handles this fine; consider pagination if list feels slow |
| Club with 0 shots after filter | Hide that club card from analytics |

## i18n Considerations
- All display strings (headers, labels, empty states) in `strings.xml`
- Distance: "yards" / "meters" from string resources
- Temperature: respect locale (°F / °C toggle — future enhancement)
- Date formatting: use `DateTimeFormatter` with device locale
- Club names: resolved from enum → string resource at render time
- Weather condition labels: resolved from `weatherCode` → string resource at render time

## Acceptance Criteria
- [ ] Club Analytics screen shows a card for each club with recorded shots
- [ ] Each card: club name, avg distance, longest, shortest, shot count
- [ ] Clubs ordered naturally (Driver → Lob Wedge)
- [ ] Shot History shows chronological list (most recent first)
- [ ] Each row: date/time, club, distance, temperature, weather, wind
- [ ] **Club filter**: multi-select, filters both views
- [ ] **Date range filter**: presets (7d, 30d, 90d, All) + custom picker
- [ ] **Weather condition filter**: multi-select
- [ ] **Temperature range filter**: min/max slider
- [ ] Active filters shown as dismissible chips
- [ ] "Clear all filters" button
- [ ] Analytics recalculate based on active filters
- [ ] Empty state for no shots recorded
- [ ] "No results" state for empty filter results
- [ ] Screens accessible via bottom navigation
- [ ] Responsive — loads quickly, no lag
- [ ] Distances in both yards and meters
