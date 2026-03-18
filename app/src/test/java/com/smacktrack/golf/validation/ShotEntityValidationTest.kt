package com.smacktrack.golf.validation

import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.domain.GpsCoordinate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Integration-style validation tests that combine multiple validation rules
 * to validate a complete shot entity.
 */
@DisplayName("Shot entity validation tests")
class ShotEntityValidationTest {

    private val validStart = GpsCoordinate(33.749, -84.388)
    private val validEnd = GpsCoordinate(33.751, -84.386)
    private val validDistance = 200.0
    private val validClub = Club.DRIVER
    private val validTimestamp = System.currentTimeMillis()

    @Test
    fun `valid complete shot with weather passes all validations`() {
        assertTrue(validateCoordinate(validStart).isValid)
        assertTrue(validateCoordinate(validEnd).isValid)
        assertTrue(validateDistance(validDistance).isValid)
        assertTrue(validateDistanceForClub(validDistance, validClub).isValid)
        assertTrue(validateTimestamp(validTimestamp).isValid)
        assertTrue(validateWeather(22.5, 10.0, 180, 1).isValid)
    }

    @Test
    fun `valid shot without weather passes all validations`() {
        assertTrue(validateCoordinate(validStart).isValid)
        assertTrue(validateCoordinate(validEnd).isValid)
        assertTrue(validateDistance(validDistance).isValid)
        assertTrue(validateDistanceForClub(validDistance, validClub).isValid)
        assertTrue(validateTimestamp(validTimestamp).isValid)
        assertTrue(validateWeather(null, null, null, null).isValid)
    }

    @Test
    fun `invalid distance fails even with valid coordinates`() {
        assertTrue(validateCoordinate(validStart).isValid)
        assertTrue(validateCoordinate(validEnd).isValid)
        assertFalse(validateDistance(600.0).isValid) // over 500 yard hard limit
    }

    @Test
    fun `invalid start coordinate fails`() {
        val invalidStart = GpsCoordinate(91.0, 0.0)
        assertFalse(validateCoordinate(invalidStart).isValid)
    }

    @Test
    fun `implausible distance for club detected`() {
        // 500 yards with a LOB_WEDGE is implausible (max 120)
        assertTrue(validateDistance(500.0).isValid) // passes hard limit
        assertFalse(validateDistanceForClub(500.0, Club.LOB_WEDGE).isValid) // fails soft limit
    }

    @Test
    fun `partial weather data fails`() {
        assertFalse(validateWeather(22.5, null, 180, 1).isValid)
    }

    @Test
    fun `each club has minimum distance validated correctly`() {
        for (club in Club.entries) {
            val range = CLUB_DISTANCE_RANGES[club]
            assertNotNull(range, "Missing range for ${club.displayName}")

            // Min of range should be valid
            assertTrue(
                validateDistanceForClub(range!!.start, club).isValid,
                "${club.displayName}: min of range (${range.start}) should be valid"
            )

            // Max of range should be valid
            assertTrue(
                validateDistanceForClub(range.endInclusive, club).isValid,
                "${club.displayName}: max of range (${range.endInclusive}) should be valid"
            )
        }
    }

    @Test
    fun `all club ranges are within hard distance limits`() {
        for (club in Club.entries) {
            val range = CLUB_DISTANCE_RANGES[club]!!
            assertTrue(
                validateDistance(range.start).isValid,
                "${club.displayName} min ${range.start} should pass hard limit"
            )
            assertTrue(
                validateDistance(range.endInclusive).isValid,
                "${club.displayName} max ${range.endInclusive} should pass hard limit"
            )
        }
    }
}
