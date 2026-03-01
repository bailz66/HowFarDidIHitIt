package com.smacktrack.golf.validation

import com.smacktrack.golf.domain.Club
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Shot distance validation tests")
class ShotDistanceValidationTest {

    // --- Hard limits (0-500 yards) ---

    @ParameterizedTest(name = "Valid distance: {0} yards")
    @ValueSource(doubles = [0.0, 1.0, 100.0, 250.0, 499.9, 500.0])
    fun `distances within hard limits pass validation`(yards: Double) {
        assertTrue(validateDistance(yards).isValid)
    }

    @ParameterizedTest(name = "Invalid distance: {0} yards")
    @ValueSource(doubles = [-1.0, -0.001, 500.1, 1000.0])
    fun `distances outside hard limits fail validation`(yards: Double) {
        assertFalse(validateDistance(yards).isValid)
    }

    @Test
    fun `NaN distance fails validation`() {
        assertFalse(validateDistance(Double.NaN).isValid)
    }

    @Test
    fun `positive infinity distance fails validation`() {
        assertFalse(validateDistance(Double.POSITIVE_INFINITY).isValid)
    }

    @Test
    fun `negative infinity distance fails validation`() {
        assertFalse(validateDistance(Double.NEGATIVE_INFINITY).isValid)
    }

    // --- Per-club plausible ranges ---

    @ParameterizedTest(name = "{0}: {1} yards is plausible")
    @CsvSource(
        "DRIVER,          200.0",
        "DRIVER,          100.0",
        "DRIVER,          400.0",
        "THREE_WOOD,      180.0",
        "FIVE_WOOD,       160.0",
        "SEVEN_WOOD,      140.0",
        "NINE_WOOD,       120.0",
        "THREE_IRON,      150.0",
        "FOUR_IRON,       140.0",
        "FIVE_IRON,       130.0",
        "SIX_IRON,        120.0",
        "SEVEN_IRON,      110.0",
        "EIGHT_IRON,      100.0",
        "NINE_IRON,        90.0",
        "PITCHING_WEDGE,   80.0",
        "GAP_WEDGE,        70.0",
        "SAND_WEDGE,       60.0",
        "LOB_WEDGE,        50.0",
        "HYBRID_3,        150.0",
        "HYBRID_4,        140.0"
    )
    fun `mid-range distance is plausible for club`(clubName: String, yards: Double) {
        val club = Club.valueOf(clubName)
        assertTrue(validateDistanceForClub(yards, club).isValid)
    }

    @ParameterizedTest(name = "{0}: {1} yards is NOT plausible")
    @CsvSource(
        "DRIVER,          50.0",   // below min 100
        "DRIVER,          450.0",  // above max 400
        "SEVEN_IRON,      10.0",   // below min 40
        "SEVEN_IRON,      250.0",  // above max 190
        "PITCHING_WEDGE,  10.0",   // below min 20
        "PITCHING_WEDGE,  200.0",  // above max 160
        "LOB_WEDGE,        1.0",   // below min 5
        "LOB_WEDGE,       150.0"   // above max 120
    )
    fun `out-of-range distance is implausible for club`(clubName: String, yards: Double) {
        val club = Club.valueOf(clubName)
        assertFalse(validateDistanceForClub(yards, club).isValid)
    }

    @ParameterizedTest(name = "{0} has a defined distance range")
    @EnumSource(Club::class)
    fun `every club has a defined plausible range`(club: Club) {
        assertNotNull(CLUB_DISTANCE_RANGES[club], "No range defined for ${club.displayName}")
    }
}
