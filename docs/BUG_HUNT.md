# Bug Hunt Methodology

## Overview

A bug hunt is a systematic, parallel audit of the entire codebase designed to catch functional bugs, security issues, dead code, UX problems, and test gaps before release. Five specialized agents run concurrently — each with a specific mandate — then their findings are synthesized into a prioritized task list and fixed sequentially.

### When to Run a Bug Hunt
- Before any release (Play Store submission, version tag)
- After a large feature merge or refactor
- When returning to the project after a break
- When the test suite grows stale relative to the codebase

### Philosophy
- **Parallel agents** — five lanes run simultaneously for speed and coverage
- **Systematic coverage** — each lane has a specific checklist so nothing falls through the cracks
- **Prioritized remediation** — findings are triaged by severity before any fixes begin
- **Verify as you go** — tests run after every fix; build verified at the end

---

## The Five Audit Lanes

| Lane | Focus | What It Looks For |
|------|-------|-------------------|
| **Functional Bugs** | Logic errors, data flow, edge cases | Race conditions, off-by-one, null handling, state machine gaps, incorrect math, unclamped values |
| **Security** | OWASP-style, Android-specific | Locale injection, unsafe deserialization, log leakage, missing auth guards, ProGuard gaps, exposed components |
| **Dead Code & Cleanup** | Unused code, duplication, stale artifacts | Unused imports/functions/variables, duplicate logic, orphan resources, stale TODO comments, unreachable branches |
| **UX Review** | User-facing issues, flow gaps | Missing feedback, confusing states, accessibility gaps, missing error recovery, edge-case UI states |
| **Test Coverage** | Gaps in test suite | Untested public functions, missing edge cases, new code without tests, stale test assertions |

---

## Agent Prompts

Use these templates when launching the five parallel agents. Each prompt tells the agent exactly what to search for and how to report findings.

### Lane 1: Functional Bugs

```
Audit all Kotlin source files under app/src/main/ for functional bugs.

Look for:
- Race conditions in coroutines (shared mutable state, missing synchronization)
- Off-by-one errors in loops, pagination, or indexing
- Null handling gaps (unsafe !! operators, missing null checks on nullable returns)
- State machine gaps in ShotTrackerViewModel (invalid phase transitions, missing resets)
- Incorrect math (distance conversions, wind calculations, GPS calibration)
- Unclamped or unvalidated values that could produce NaN, Infinity, or negative distances
- Edge cases: empty lists, single-element lists, zero values, boundary values
- Firestore sync issues (missing null checks on UID, data loss on conflict)

For each finding, report:
- File and line number
- Description of the bug
- Severity (Critical / High / Medium / Low)
- Suggested fix
```

### Lane 2: Security

```
Audit all Kotlin source files under app/src/main/ for security vulnerabilities.

Look for:
- Locale injection: String.format() without Locale.US for API URLs or numeric formatting
- Unsafe deserialization: getInt/getString instead of optInt/optString in JSON parsing
- Log leakage: Log.d/Log.i/Log.v calls that expose sensitive data (coordinates, tokens, user IDs)
- Missing auth guards: Firestore operations without null UID checks
- ProGuard gaps: classes/methods that should be kept but aren't in proguard-rules.pro
- Exposed Android components: activities/services/receivers without proper permissions
- Hardcoded secrets: API keys, tokens, credentials in source code
- Insecure network: HTTP (not HTTPS), missing certificate pinning for sensitive APIs
- Intent injection: unvalidated extras from incoming intents

For each finding, report:
- File and line number
- Vulnerability type (OWASP category if applicable)
- Severity (Critical / High / Medium / Low)
- Suggested fix
```

### Lane 3: Dead Code & Cleanup

```
Audit all Kotlin source files under app/src/main/ for dead code and cleanup opportunities.

Look for:
- Unused imports, functions, variables, and parameters
- Duplicate logic (same calculation or pattern repeated in multiple places)
- Orphan resources: drawables, strings, colors defined but never referenced
- Stale TODO/FIXME/HACK comments that reference completed work
- Unreachable code branches (always-true/false conditions, dead else blocks)
- Unused dependencies in build.gradle.kts
- Empty catch blocks or swallowed exceptions
- Commented-out code blocks

For each finding, report:
- File and line number
- What is unused/duplicate/stale
- Severity (Medium for dead code, Low for cleanup)
- Suggested action (delete, consolidate, or extract)
```

### Lane 4: UX Review

```
Audit all UI-related Kotlin files under app/src/main/java/.../ui/ and screen/ for UX issues.

Look for:
- Missing user feedback: actions with no loading indicator, no success/error feedback
- Confusing states: screens that show stale data, empty states without guidance
- Missing error recovery: network failures, GPS unavailable, permission denied without retry option
- Accessibility: missing contentDescription on icons/images, insufficient color contrast, tiny touch targets
- Edge-case UI: what happens with 0 shots, 1 shot, 1000 shots? Very long club names? Extreme distances?
- Navigation gaps: dead-end screens, missing back navigation, inconsistent tab behavior
- Animation/transition issues: jarring state changes, missing transitions between phases

For each finding, report:
- Screen and component affected
- Description of the UX issue
- Severity (High for blocking issues, Medium for confusing UX, Low for polish)
- Suggested improvement
```

### Lane 5: Test Coverage

```
Audit test coverage by comparing app/src/main/ against app/src/test/.

Look for:
- Public functions in main/ that have no corresponding test
- New or recently modified code without test coverage
- Missing edge cases in existing tests (empty inputs, null values, boundary values)
- Test files that test deprecated or removed functionality
- Missing negative tests (invalid inputs, error paths)
- Untested error handling and fallback paths
- Integration gaps: components that interact but aren't tested together

For each finding, report:
- Source file and function missing coverage
- What test cases are needed
- Severity (High for critical paths, Medium for utility functions, Low for UI helpers)
- Suggested test approach
```

---

## Synthesis & Prioritization

After all five agents complete, synthesize their findings into a single prioritized task list.

### Priority Levels

| Priority | Criteria | Action |
|----------|----------|--------|
| **Critical** | Data loss, security vulnerability, crash, corrupted state | Fix immediately — blocks release |
| **High** | Incorrect behavior, bad UX that confuses users, missing validation | Fix before release |
| **Medium** | Dead code cleanup, minor UX polish, non-critical test gaps | Fix if time permits |
| **Low** | Style issues, nice-to-have improvements, minor cleanup | Add to backlog |

### Deduplication
Agents often find the same issue from different angles. When synthesizing:
- Merge overlapping findings (e.g., "unsafe deserialization" from Security + "missing null check" from Functional)
- Keep the highest severity rating from any lane
- Combine the suggested fixes into a single actionable task

### Output Format
Create a numbered task list sorted by priority:

```
1. [Critical] Fix race condition in ShotRepository.sync() — concurrent writes can lose shots
2. [Critical] Add Locale.US to WeatherService URL formatting — locale injection on EU devices
3. [High] Add null check before Firestore UID access in AuthManager.signOut()
4. [High] Show error state when GPS is unavailable during calibration
5. [Medium] Remove unused `calculateLegacyDistance()` in HaversineCalculator
6. [Medium] Add tests for WindCalculator.analyze() with zero wind speed
7. [Low] Clean up stale TODO in ShotTrackerScreen line 245
```

---

## Execution Pattern

### Step 1: Launch Agents
Launch all five audit agents in parallel using the Agent tool. Each gets its lane prompt from the templates above.

### Step 2: Collect Reports
Wait for all agents to complete. Each returns a list of findings with file locations, descriptions, and severity ratings.

### Step 3: Synthesize
Merge the five reports into a single deduplicated, prioritized task list (see Synthesis above).

### Step 4: Create Tasks
Create numbered tasks from the prioritized list. Work through them sequentially, starting with Critical items.

### Step 5: Fix and Verify
For each task:
1. Read the affected file(s)
2. Implement the fix
3. Run the test suite: `./gradlew testDebugUnitTest`
4. Verify the fix doesn't break anything
5. Mark the task complete and move to the next

### Step 6: Final Build
After all fixes are applied, run the full verification checklist below.

---

## Verification Checklist

Run through this checklist after completing all bug hunt fixes:

- [ ] All unit tests pass: `./gradlew testDebugUnitTest`
- [ ] Debug build succeeds: `./gradlew assembleDebug`
- [ ] Release build succeeds: `./gradlew assembleRelease`
- [ ] No new lint warnings introduced
- [ ] App launches and completes core flow (Smack → Walk → Track → Result)
- [ ] Shot history displays correctly after fixes
- [ ] Analytics tab loads without errors
- [ ] Settings persist across app restart
- [ ] Cloud sync works (if signed in)
- [ ] No regressions in recently fixed areas

---

## Example Findings

Real examples from past bug hunts showing what each lane typically catches.

### Functional Bugs
| Finding | File | Fix |
|---------|------|-----|
| `String.format()` without `Locale.US` in API URL construction | `WeatherService.kt` | Added `Locale.US` — prevents comma decimal separators on EU locales |
| `toInt()` truncation instead of `roundToInt()` for wind speed | `WindCalculator.kt` | Changed to `roundToInt()` — 14.7 mph was displaying as 14 |
| Weather fallback used 0°F baseline (unrealistic wind density) | `ShotTrackerViewModel.kt` | Changed fallback to 70°F (standard conditions) |
| HOT_STREAK achievement counted current shot in running average | `AchievementChecker.kt` | Fixed to use prior shots only for average calculation |

### Security
| Finding | File | Fix |
|---------|------|-----|
| `getString()` instead of `optString()` in JSON parsing | `ShotSerialization.kt` | Switched to `opt*()` methods with safe defaults |
| Firestore sync called without null UID check | `ShotRepository.kt` | Added `if (uid != null)` guard before all sync operations |
| `Log.d` calls with GPS coordinates in release builds | Multiple files | ProGuard rule strips `Log.v`, `Log.d`, `Log.i` in release |

### Dead Code & Cleanup
| Finding | File | Fix |
|---------|------|-----|
| Unused color resources defined in `colors.xml` | `colors.xml` | Removed unreferenced color definitions |
| Duplicate distance formatting logic in two screens | `AnalyticsScreen.kt`, `HistoryScreen.kt` | Consolidated into `ShotDisplayUtils.kt` |
| Stale `backup_rules.xml` no longer referenced | `res/xml/` | Deleted orphan file |

### UX Review
| Finding | Screen | Fix |
|---------|--------|-----|
| No GPS accuracy warning on result screen | `ShotTrackerScreen.kt` | Added accuracy indicator, warns if >15m |
| No timeout for abandoned shots during walking phase | `ShotTrackerViewModel.kt` | Added 15-minute auto-reset with toast |
| Screen turns off during walking phase, losing GPS | `MainActivity.kt` | Added `FLAG_KEEP_SCREEN_ON` + foreground service |

### Test Coverage
| Finding | Missing Test | Added |
|---------|-------------|-------|
| `WindCalculator.analyze()` untested with trajectory settings | Trajectory multiplier tests | `WindCalculatorTrajectoryTest.kt` (12 tests) |
| Bearing calculation untested for edge cases | Date line, poles, cardinal directions | `BearingTest.kt` (16 tests) |
| `weatherGroup` mapping untested | WMO code → group mapping | `WeatherGroupTest.kt` (28 mappings) |
| `GpsCalibrator.calibrateWeighted()` untested | Accuracy weighting, gating, outliers | `CalibrateWeightedTest.kt` (22 tests) |

---

## Related Documentation
- [Testing Strategy](./TESTING.md) — test framework, conventions, and inventory
- [Code Quality](./CODE_QUALITY.md) — coding standards and static analysis
- [Architecture](./ARCHITECTURE.md) — system design and layer boundaries
