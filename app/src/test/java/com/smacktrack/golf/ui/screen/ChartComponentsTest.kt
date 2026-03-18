package com.smacktrack.golf.ui.screen

import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ChartComponents utility function tests")
class ChartComponentsTest {

    private fun shot(timestampMs: Long, distanceYards: Int = 150) = ShotResult(
        club = Club.SEVEN_IRON,
        distanceYards = distanceYards,
        distanceMeters = (distanceYards * 0.9144).toInt(),
        weatherDescription = "Clear",
        temperatureF = 72,
        temperatureC = 22,
        windSpeedKmh = 10.0,
        windDirectionCompass = "N",
        timestampMs = timestampMs
    )

    // ── groupIntoSessions ───────────────────────────────────────────────────

    @Nested
    @DisplayName("groupIntoSessions")
    inner class GroupIntoSessionsTests {

        @Test
        @DisplayName("empty list returns empty sessions")
        fun emptyList() {
            val result = groupIntoSessions(emptyList<ShotResult>())
            assertEquals(0, result.size)
        }

        @Test
        @DisplayName("single shot returns one session with one shot")
        fun singleShot() {
            val shots = listOf(shot(1000))
            val sessions = groupIntoSessions(shots)
            assertEquals(1, sessions.size)
            assertEquals(1, sessions[0].shots.size)
        }

        @Test
        @DisplayName("two shots within 30 minutes are grouped into one session")
        fun twoShotsWithin30Min() {
            val shots = listOf(
                shot(1000),
                shot(1000 + 29 * 60 * 1000L)
            )
            val sessions = groupIntoSessions(shots)
            assertEquals(1, sessions.size)
            assertEquals(2, sessions[0].shots.size)
        }

        @Test
        @DisplayName("two shots more than 30 minutes apart are split into two sessions")
        fun twoShotsApart() {
            val shots = listOf(
                shot(1000),
                shot(1000 + 31 * 60 * 1000L)
            )
            val sessions = groupIntoSessions(shots)
            assertEquals(2, sessions.size)
            assertEquals(1, sessions[0].shots.size)
            assertEquals(1, sessions[1].shots.size)
        }

        @Test
        @DisplayName("boundary exact: gap == 30min stays in same session")
        fun boundaryExactSameSession() {
            val gap = 30 * 60 * 1000L
            val shots = listOf(shot(1000), shot(1000 + gap))
            val sessions = groupIntoSessions(shots)
            assertEquals(1, sessions.size)
        }

        @Test
        @DisplayName("boundary: gap == 30min + 1ms splits into two sessions")
        fun boundaryPlusOneSplits() {
            val gap = 30 * 60 * 1000L + 1
            val shots = listOf(shot(1000), shot(1000 + gap))
            val sessions = groupIntoSessions(shots)
            assertEquals(2, sessions.size)
        }
    }

    // ── computeTrend ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("computeTrend")
    inner class ComputeTrendTests {

        @Test
        @DisplayName("fewer than 4 shots returns FLAT")
        fun fewerThanFour() {
            assertEquals(TrendDirection.FLAT, computeTrend(listOf(100, 110, 120)))
        }

        @Test
        @DisplayName("UP when recent 3 average exceeds overall average by more than 3")
        fun trendUp() {
            // 100, 100, 100, 120, 120, 120 → overall avg = 110, recent 3 avg = 120 → diff = 10 > 3
            val distances = listOf(100, 100, 100, 120, 120, 120)
            assertEquals(TrendDirection.UP, computeTrend(distances))
        }

        @Test
        @DisplayName("DOWN when recent 3 average is below overall average by more than 3")
        fun trendDown() {
            // 120, 120, 120, 100, 100, 100 → overall avg = 110, recent 3 avg = 100 → diff = -10 < -3
            val distances = listOf(120, 120, 120, 100, 100, 100)
            assertEquals(TrendDirection.DOWN, computeTrend(distances))
        }

        @Test
        @DisplayName("FLAT when difference is exactly +3")
        fun flatAtBoundaryPositive() {
            // Need: recent3_avg - overall_avg == 3
            // [97, 97, 97, 100, 100, 100] → overall = 98.5, recent3 = 100 → diff = 1.5 → FLAT
            // Try: [94, 94, 94, 100, 100, 100] → overall = 97, recent3 = 100 → diff = 3 → FLAT (not > 3)
            val distances = listOf(94, 94, 94, 100, 100, 100)
            assertEquals(TrendDirection.FLAT, computeTrend(distances))
        }

        @Test
        @DisplayName("FLAT when difference is exactly -3")
        fun flatAtBoundaryNegative() {
            val distances = listOf(100, 100, 100, 94, 94, 94)
            assertEquals(TrendDirection.FLAT, computeTrend(distances))
        }
    }

    // ── stdDev ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("stdDev")
    inner class StdDevTests {

        @Test
        @DisplayName("single value returns 0")
        fun singleValue() {
            assertEquals(0, stdDev(listOf(100)))
        }

        @Test
        @DisplayName("identical values return 0")
        fun identicalValues() {
            assertEquals(0, stdDev(listOf(100, 100, 100, 100)))
        }

        @Test
        @DisplayName("known set computes correct std dev")
        fun knownSet() {
            // [100, 110, 120] → mean = 110
            // variance = ((100-110)^2 + (110-110)^2 + (120-110)^2) / 3 = (100+0+100)/3 = 66.67
            // stddev = sqrt(66.67) = 8.16 → toInt() = 8
            assertEquals(8, stdDev(listOf(100, 110, 120)))
        }
    }

    // ── computeSessionSummary ────────────────────────────────────────────────

    @Nested
    @DisplayName("computeSessionSummary")
    inner class ComputeSessionSummaryTests {

        private fun shotWith(ts: Long, yards: Int, club: Club = Club.SEVEN_IRON) = ShotResult(
            club = club,
            distanceYards = yards,
            distanceMeters = (yards * 0.9144).toInt(),
            weatherDescription = "Clear",
            temperatureF = 72,
            temperatureC = 22,
            windSpeedKmh = 10.0,
            windDirectionCompass = "N",
            timestampMs = ts
        )

        @Test
        @DisplayName("fewer than 3 shots returns null")
        fun fewerThanThree() {
            val shots = listOf(shotWith(1000, 150), shotWith(2000, 160))
            assertNull(computeSessionSummary(shots, DistanceUnit.YARDS))
        }

        @Test
        @DisplayName("5 shots computes correct summary")
        fun fiveShots() {
            val shots = listOf(
                shotWith(1000, 140, Club.SEVEN_IRON),
                shotWith(2000, 160, Club.SEVEN_IRON),
                shotWith(3000, 200, Club.DRIVER),
                shotWith(4000, 180, Club.FIVE_IRON),
                shotWith(5000, 170, Club.SEVEN_IRON)
            )
            val summary = computeSessionSummary(shots, DistanceUnit.YARDS)
            assertNotNull(summary)
            assertEquals(5, summary!!.totalShots)
            assertEquals(170, summary.avgDistance) // (140+160+200+180+170)/5 = 850/5 = 170
            assertEquals(Club.DRIVER, summary.bestClub)
            assertEquals(200, summary.bestDistance)
            assertEquals(3, summary.clubsUsedCount) // 7-iron, Driver, 5-iron
        }

        @Test
        @DisplayName("meters unit uses distanceMeters")
        fun metersUnit() {
            val shots = listOf(
                shotWith(1000, 200), // meters = 182
                shotWith(2000, 150), // meters = 137
                shotWith(3000, 100)  // meters = 91
            )
            val summary = computeSessionSummary(shots, DistanceUnit.METERS)
            assertNotNull(summary)
            assertEquals(137, summary!!.avgDistance) // round((182+137+91)/3) = round(136.67) = 137
        }
    }

    // ── currentActiveSession ────────────────────────────────────────────────

    @Nested
    @DisplayName("currentActiveSession")
    inner class CurrentActiveSessionTests {

        @Test
        @DisplayName("empty list returns null")
        fun emptyList() {
            assertNull(currentActiveSession(emptyList<ShotResult>()))
        }

        @Test
        @DisplayName("shot older than 30 minutes returns null")
        fun oldShot() {
            val oldTs = System.currentTimeMillis() - 31 * 60 * 1000L
            assertNull(currentActiveSession(listOf(shot(oldTs))))
        }

        @Test
        @DisplayName("recent shot returns a session")
        fun recentShot() {
            val recentTs = System.currentTimeMillis() - 5 * 60 * 1000L
            val session = currentActiveSession(listOf(shot(recentTs)))
            assertNotNull(session)
            assertEquals(1, session!!.shots.size)
        }

        @Test
        @DisplayName("returns only the last session when multiple exist")
        fun multipleSessionsReturnsLast() {
            val now = System.currentTimeMillis()
            val oldTs = now - 60 * 60 * 1000L // 1 hour ago (separate session)
            val recentTs = now - 5 * 60 * 1000L
            val shots = listOf(shot(oldTs, 100), shot(recentTs, 200))
            val session = currentActiveSession(shots)
            assertNotNull(session)
            assertEquals(1, session!!.shots.size)
            assertEquals(200, session.shots[0].distanceYards)
        }

        @Test
        @DisplayName("boundary: shot exactly 30 min ago returns null")
        fun boundaryExact30Min() {
            val ts = System.currentTimeMillis() - 30 * 60 * 1000L
            // Gap > SESSION_GAP_MS because System.currentTimeMillis() advances
            // Use 30min + 1ms to guarantee it's past the boundary
            val ts2 = System.currentTimeMillis() - 30 * 60 * 1000L - 1
            assertNull(currentActiveSession(listOf(shot(ts2))))
        }

        @Test
        @DisplayName("multiple recent shots grouped into one session")
        fun multipleRecentShots() {
            val now = System.currentTimeMillis()
            val shots = listOf(
                shot(now - 10 * 60 * 1000L, 140),
                shot(now - 5 * 60 * 1000L, 160),
                shot(now - 1 * 60 * 1000L, 180)
            )
            val session = currentActiveSession(shots)
            assertNotNull(session)
            assertEquals(3, session!!.shots.size)
        }
    }
}
