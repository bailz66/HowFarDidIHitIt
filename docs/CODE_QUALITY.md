# Code Quality Standards

## Overview
This document defines the coding standards, conventions, and quality gates for the project. All contributors (human and AI) should follow these guidelines.

## Language & Style

### Kotlin Conventions
- Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Follow [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide)
- Use **expression bodies** for single-expression functions
- Prefer `val` over `var` — immutability by default
- Use `data class` for models and state objects
- Use `sealed class` / `sealed interface` for restricted hierarchies (UI state, navigation)
- Use `enum class` with properties for fixed sets (clubs, weather codes)
- Avoid `!!` (non-null assertion) — use `?.let`, `?:`, or explicit null checks
- Use `when` instead of `if-else` chains for 3+ branches

### Naming Conventions
| Element | Convention | Example |
|---------|-----------|---------|
| Package | lowercase, dot-separated | `com.example.howfardidihitit.data` |
| Class | PascalCase | `ShotTrackerViewModel` |
| Function | camelCase | `calculateDistance()` |
| Property | camelCase | `distanceYards` |
| Constant | SCREAMING_SNAKE_CASE | `GPS_SAMPLE_DURATION_MS` |
| Composable | PascalCase (noun) | `ShotResultCard()` |
| State | camelCase with `uiState` suffix | `shotTrackerUiState` |
| ViewModel | PascalCase with `ViewModel` suffix | `AnalyticsViewModel` |

### Compose Conventions
- Composable functions are named as **nouns** (they describe UI, not actions)
- Pass `Modifier` as the first optional parameter
- Hoist state out of composables — composables should be stateless where possible
- Use `remember` and `derivedStateOf` for computed values
- Preview every screen and major component with `@Preview`

```kotlin
// Good
@Composable
fun ShotResultCard(
    shot: Shot,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) { ... }

// Bad — verb name, no modifier parameter
@Composable
fun ShowShotResult(shot: Shot, onDismiss: () -> Unit) { ... }
```

## Architecture Rules

### Layer Boundaries
- **UI layer** (screens, composables) NEVER directly accesses Room or Retrofit
- **ViewModels** access data through **repositories only**
- **Repositories** are the single source of truth for data
- **Services** (Location, Weather) are injected via Hilt, not instantiated directly

### State Management
- UI state is an **immutable data class** exposed as `StateFlow<T>`
- State updates happen via `copy()` on the data class
- Side effects (navigation, toasts) flow through `SharedFlow` or `Channel`
- No mutable state exposed from ViewModels

```kotlin
// UI State
data class ShotTrackerUiState(
    val selectedClub: Club? = null,
    val startPin: GpsCoordinate? = null,
    val endPin: GpsCoordinate? = null,
    val liveDistanceYards: Double? = null,
    val isCalibrating: Boolean = false,
    val shotResult: ShotResult? = null,
    val error: String? = null
)
```

### Coroutines
- Use `viewModelScope` for ViewModel coroutines
- Use `Dispatchers.IO` for database and network calls
- Use `Dispatchers.Default` for CPU-intensive calculations (GPS calibration math)
- Never use `GlobalScope`
- Handle cancellation gracefully

## Static Analysis

### Lint
- Android Lint runs on every build
- **Zero warnings** policy for new code — fix warnings, don't suppress them
- Suppress only with documented justification: `@Suppress("reason: ...")`

### Detekt (Kotlin Static Analysis)
Configuration in `detekt.yml`:
- Max function length: 30 lines (composables excluded)
- Max class length: 200 lines
- Complexity threshold: 10 (cyclomatic)
- No magic numbers — use named constants
- No wildcard imports

### ktlint (Formatting)
- Enforced via pre-commit hook or CI
- Standard Kotlin style — no custom rules
- Max line length: 120 characters
- Trailing commas in multiline declarations

## Documentation Standards

### When to Document
- **Public API** of each module (repository methods, ViewModel functions)
- **Complex algorithms** (GPS calibration, Haversine formula)
- **Non-obvious decisions** (why we chose X over Y)
- **DO NOT** document obvious code (getters, simple CRUD, standard patterns)

### KDoc Format
```kotlin
/**
 * Calculates the great-circle distance between two GPS coordinates
 * using the Haversine formula.
 *
 * @param start The starting coordinate (calibrated)
 * @param end The ending coordinate (calibrated)
 * @return Distance in meters. Convert with [metersToYards] for display.
 */
fun calculateDistance(start: GpsCoordinate, end: GpsCoordinate): Double
```

## Git Conventions

### Branch Naming
```
feature/shot-tracking
feature/weather-api
feature/analytics-filters
bugfix/gps-calibration-outliers
chore/update-dependencies
```

### Commit Messages
- Use imperative mood: "Add GPS calibration" not "Added GPS calibration"
- First line: < 72 characters, summarizes the change
- Body (optional): explains **why**, not **what**
- Reference issue numbers: `Implements #2` or `Fixes #15`

### Pull Requests
- One feature or fix per PR
- PR description includes: summary, test plan, screenshots (for UI changes)
- All CI checks must pass before merge
- Squash merge to keep history clean

## Dependency Policy
- Prefer official Jetpack / Google libraries over third-party
- Every new dependency must be justified — no "just in case" libraries
- Pin dependency versions in `libs.versions.toml`
- Update dependencies monthly — check for security advisories
- No snapshot or alpha dependencies in release builds

## String Handling for i18n
- **NEVER** hardcode user-visible strings in Kotlin code
- All user-visible text goes in `res/values/strings.xml`
- Use parameterized strings for dynamic content: `<string name="shot_distance">%1$d yards (%2$d m)</string>`
- Store data values (club names in DB, weather codes) as **language-neutral keys**, not display text
- See [Internationalization](./INTERNATIONALIZATION.md) for full details

## Performance Guidelines
- Compose: avoid recomposition of expensive composables — use `key()` and `remember`
- Room queries return `Flow<T>` — observe reactively, don't poll
- GPS: don't request high-frequency updates when the app is idle
- Images: use vector drawables where possible, compress bitmaps
- Startup: keep `onCreate` fast — defer non-critical initialization

## Branch Protection Rules

### `master` Branch Protection
Configure in GitHub Settings → Branches → Branch protection rules:

| Rule | Setting |
|------|---------|
| Require pull request before merging | **Enabled** |
| Required approvals | 1 (increase as team grows) |
| Dismiss stale PR approvals on new push | **Enabled** |
| Require status checks to pass | **Enabled** |
| Required status checks | `lint`, `unit-test`, `build` |
| Require branch to be up to date | **Enabled** |
| Restrict force pushes | **Enabled** (no force push) |
| Restrict deletions | **Enabled** (no branch deletion) |

### `release/*` Branch Protection
| Rule | Setting |
|------|---------|
| Require pull request before merging | **Enabled** |
| Required status checks | `lint`, `unit-test`, `build` |
| Restrict force pushes | **Enabled** |
| Allowed merge sources | `hotfix/*` branches only |

## Merge Strategy Enforcement

| Merge Type | Strategy | Rationale |
|-----------|----------|-----------|
| Feature → `master` | **Squash merge** | Produces a clean, linear commit history |
| Bugfix → `master` | **Squash merge** | One commit per fix in history |
| Hotfix → `release/*` | **Merge commit** | Preserves full fix history for audit |
| Hotfix → `master` | **Cherry-pick** | Applies fix without pulling in release-specific commits |

Configure in GitHub Settings → General → Pull Requests:
- **Allow squash merging**: Enabled (default for feature/bugfix PRs)
- **Allow merge commits**: Enabled (for hotfix → release merges)
- **Allow rebase merging**: Disabled (prevents rewriting shared history)
- **Automatically delete head branches**: Enabled (clean up after merge)

## Required CI Status Checks

The following checks must pass before any PR can be merged:

| Check Name | Job | What It Validates |
|-----------|-----|-------------------|
| `lint` | `ci.yml` | Android Lint — no warnings or errors |
| `unit-test` | `ci.yml` | All JUnit 5 unit tests pass |
| `build` | `ci.yml` | Debug APK builds successfully |

Instrumented tests (`instrumented-test`) run but are not required for merge, as emulator tests can be flaky in CI. They serve as an additional confidence signal.

See [Branching Strategy](./BRANCHING_STRATEGY.md) for the full GitLab Flow workflow and branch naming conventions.
