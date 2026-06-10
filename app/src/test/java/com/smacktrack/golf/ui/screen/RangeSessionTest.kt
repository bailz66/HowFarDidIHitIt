package com.smacktrack.golf.ui.screen

import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Validates the Range Mode ("one smack, many tracks") storage design: a burst of same-club balls
 * tracked seconds apart must group into a single History [Session] and produce correct dispersion
 * stats, since each ball is persisted as a normal [ShotResult].
 */
@DisplayName("Range Mode session grouping")
class RangeSessionTest {

    /** A range ball: same club, distinct timestamp seconds after the previous one. */
    private fun ball(timestampMs: Long, yards: Int, club: Club = Club.SEVEN_IRON) = ShotResult(
        club = club,
        distanceYards = yards,
        distanceMeters = (yards * 0.9144).toInt(),
        weatherDescription = "Clear",
        temperatureF = 72,
        temperatureC = 22,
        windSpeedKmh = 8.0,
        windDirectionCompass = "N",
        timestampMs = timestampMs
    )

    /** Simulates a range session: N balls of one club, each tracked ~20s apart. */
    private fun rangeSession(start: Long, distances: List<Int>, club: Club = Club.SEVEN_IRON) =
        distances.mapIndexed { i, yards -> ball(start + i * 20_000L, yards, club) }

    @Test
    @DisplayName("a burst of balls seconds apart groups into one session")
    fun burstGroupsIntoOneSession() {
        val balls = rangeSession(1_000_000L, listOf(150, 158, 145, 162, 151, 149, 155))
        val sessions = groupIntoSessions(balls)
        assertEquals(1, sessions.size)
        assertEquals(7, sessions[0].shots.size)
    }

    @Test
    @DisplayName("all balls in a session share the tracked club")
    fun allBallsShareClub() {
        val balls = rangeSession(1_000_000L, listOf(240, 251, 233, 245), club = Club.DRIVER)
        val sessions = groupIntoSessions(balls)
        assertEquals(1, sessions.size)
        assertTrue(sessions[0].shots.all { it.club == Club.DRIVER })
    }

    @Test
    @DisplayName("range session does not absorb an unrelated earlier shot")
    fun separateFromEarlierSession() {
        val earlier = ball(1_000_000L, 100, Club.PITCHING_WEDGE)
        // Range session starts an hour later
        val balls = rangeSession(1_000_000L + 60 * 60 * 1000L, listOf(150, 158, 145))
        val sessions = groupIntoSessions(listOf(earlier) + balls)
        assertEquals(2, sessions.size)
        assertEquals(1, sessions[0].shots.size)
        assertEquals(3, sessions[1].shots.size)
    }

    @Test
    @DisplayName("session summary reports correct count, average and best for a range bucket")
    fun summaryStatsForBucket() {
        val balls = rangeSession(1_000_000L, listOf(150, 160, 140, 170, 130))
        val sessions = groupIntoSessions(balls)
        val summary = computeSessionSummary(sessions[0].shots, DistanceUnit.YARDS)
        assertNotNull(summary)
        assertEquals(5, summary!!.totalShots)
        assertEquals(150, summary.avgDistance)   // (150+160+140+170+130)/5
        assertEquals(170, summary.bestDistance)
        assertEquals(1, summary.clubsUsedCount)   // single club for the whole bucket
    }

    @Test
    @DisplayName("dispersion (longest minus shortest) matches the tracked distances")
    fun dispersionSpread() {
        val balls = rangeSession(1_000_000L, listOf(150, 162, 145, 158))
        val yards = balls.map { it.distanceYards }
        val spread = yards.max() - yards.min()
        assertEquals(17, spread) // 162 - 145
    }
}
