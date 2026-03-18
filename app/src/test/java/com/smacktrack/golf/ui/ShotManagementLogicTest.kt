package com.smacktrack.golf.ui

import com.smacktrack.golf.domain.Club
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for pure logic extracted from ShotTrackerViewModel:
 * - Shot deletion by timestamp
 * - Wind direction wrapping
 * - Wind speed clamping
 * - Club toggle last-club guard
 * - Enabled clubs empty fallback
 */
@DisplayName("Shot management logic tests")
class ShotManagementLogicTest {

    private fun shot(
        club: Club = Club.SEVEN_IRON,
        distanceYards: Int = 150,
        timestampMs: Long = 1700000000000L
    ) = ShotResult(
        club = club,
        distanceYards = distanceYards,
        distanceMeters = (distanceYards * 0.9144).toInt(),
        weatherDescription = "Clear sky",
        temperatureF = 72,
        temperatureC = 22,
        windSpeedKmh = 16.0,
        windDirectionCompass = "NW",
        windDirectionDegrees = 315,
        shotBearingDegrees = 45.0,
        timestampMs = timestampMs
    )

    // ── Delete by timestamp ──────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteShot by timestampMs")
    inner class DeleteByTimestamp {

        @Test
        @DisplayName("removes shot with matching timestamp")
        fun removesMatchingShot() {
            val history = listOf(shot(timestampMs = 100), shot(timestampMs = 200), shot(timestampMs = 300))
            val result = history.filter { it.timestampMs != 200L }
            assertEquals(2, result.size)
            assertTrue(result.none { it.timestampMs == 200L })
        }

        @Test
        @DisplayName("leaves history unchanged if no match")
        fun noMatchUnchanged() {
            val history = listOf(shot(timestampMs = 100), shot(timestampMs = 200))
            val result = history.filter { it.timestampMs != 999L }
            assertEquals(2, result.size)
        }

        @Test
        @DisplayName("empty history stays empty")
        fun emptyHistoryStaysEmpty() {
            val result = emptyList<ShotResult>().filter { it.timestampMs != 100L }
            assertEquals(0, result.size)
        }

        @Test
        @DisplayName("removes all shots with duplicate timestamps")
        fun removesDuplicateTimestamps() {
            val history = listOf(
                shot(club = Club.DRIVER, timestampMs = 100),
                shot(club = Club.SEVEN_IRON, timestampMs = 100),
                shot(timestampMs = 200)
            )
            val result = history.filter { it.timestampMs != 100L }
            assertEquals(1, result.size)
            assertEquals(200L, result[0].timestampMs)
        }
    }

    // ── Wind direction wrapping ──────────────────────────────────────────────

    @Nested
    @DisplayName("wind direction wrapping")
    inner class WindDirectionWrapping {

        private fun wrap(degrees: Int, delta: Int): Int =
            (degrees + delta + 360) % 360

        @Test
        @DisplayName("350 + 15 wraps to 5")
        fun wrapClockwisePast360() {
            assertEquals(5, wrap(350, 15))
        }

        @Test
        @DisplayName("5 - 15 wraps to 350")
        fun wrapCounterClockwisePast0() {
            assertEquals(350, wrap(5, -15))
        }

        @Test
        @DisplayName("0 + 45 = 45")
        fun normalClockwise() {
            assertEquals(45, wrap(0, 45))
        }

        @Test
        @DisplayName("0 - 45 wraps to 315")
        fun wrapCounterClockwiseFrom0() {
            assertEquals(315, wrap(0, -45))
        }

        @Test
        @DisplayName("315 + 45 = 0")
        fun fullCircleToZero() {
            assertEquals(0, wrap(315, 45))
        }
    }

    // ── Wind speed clamping ──────────────────────────────────────────────────

    @Nested
    @DisplayName("wind speed clamping")
    inner class WindSpeedClamping {

        private fun clamp(current: Double, delta: Double): Double =
            (current + delta).coerceIn(0.0, 100.0)

        @Test
        @DisplayName("cannot go below 0")
        fun floorAtZero() {
            assertEquals(0.0, clamp(5.0, -10.0))
        }

        @Test
        @DisplayName("cannot exceed 100")
        fun ceilingAt100() {
            assertEquals(100.0, clamp(95.0, 10.0))
        }

        @Test
        @DisplayName("normal increment works")
        fun normalIncrement() {
            assertEquals(15.0, clamp(10.0, 5.0))
        }

        @Test
        @DisplayName("normal decrement works")
        fun normalDecrement() {
            assertEquals(5.0, clamp(10.0, -5.0))
        }

        @Test
        @DisplayName("0 + 0 stays at 0")
        fun zeroStaysZero() {
            assertEquals(0.0, clamp(0.0, 0.0))
        }
    }

    // ── Club toggle last-club guard ──────────────────────────────────────────

    @Nested
    @DisplayName("toggleClub last-club guard")
    inner class ToggleClub {

        private fun toggle(current: Set<Club>, club: Club): Set<Club> {
            if (club in current && current.size <= 1) return current
            return if (club in current) current - club else current + club
        }

        @Test
        @DisplayName("toggling enabled club removes it")
        fun toggleRemoves() {
            val result = toggle(setOf(Club.DRIVER, Club.SEVEN_IRON), Club.DRIVER)
            assertEquals(setOf(Club.SEVEN_IRON), result)
        }

        @Test
        @DisplayName("toggling disabled club adds it")
        fun toggleAdds() {
            val result = toggle(setOf(Club.DRIVER), Club.SEVEN_IRON)
            assertEquals(setOf(Club.DRIVER, Club.SEVEN_IRON), result)
        }

        @Test
        @DisplayName("cannot disable the last remaining club")
        fun cannotDisableLast() {
            val result = toggle(setOf(Club.DRIVER), Club.DRIVER)
            assertEquals(setOf(Club.DRIVER), result)
        }

        @Test
        @DisplayName("toggling twice returns to original")
        fun toggleTwiceReturnsOriginal() {
            val original = setOf(Club.DRIVER, Club.SEVEN_IRON)
            val afterFirst = toggle(original, Club.DRIVER)
            val afterSecond = toggle(afterFirst, Club.DRIVER)
            assertEquals(original, afterSecond)
        }
    }

    // ── Enabled clubs empty fallback ─────────────────────────────────────────

    @Nested
    @DisplayName("enabledClubs empty fallback")
    inner class EnabledClubsFallback {

        private fun withFallback(parsed: Set<Club>): Set<Club> =
            parsed.ifEmpty { Club.entries.toSet() }

        @Test
        @DisplayName("empty set falls back to all clubs")
        fun emptyFallsBack() {
            assertEquals(Club.entries.toSet(), withFallback(emptySet()))
        }

        @Test
        @DisplayName("non-empty set is preserved")
        fun nonEmptyPreserved() {
            val custom = setOf(Club.DRIVER, Club.LOB_WEDGE)
            assertEquals(custom, withFallback(custom))
        }

        @Test
        @DisplayName("single club set is preserved")
        fun singleClubPreserved() {
            val single = setOf(Club.SEVEN_IRON)
            assertEquals(single, withFallback(single))
        }

        @Test
        @DisplayName("all clubs set is preserved")
        fun allClubsPreserved() {
            val all = Club.entries.toSet()
            assertEquals(all, withFallback(all))
        }
    }
}
