package com.smacktrack.golf.data

import com.smacktrack.golf.location.metersToYards
import com.smacktrack.golf.location.yardsToMeters
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Distance formatting boundary value tests")
class DistanceFormattingBoundaryTest {

    @ParameterizedTest(name = "{0}m = ~{1}yds")
    @CsvSource(
        "0.0,       0.0,     0.001",
        "0.9144,    1.0,     0.001",  // 1 yard in meters
        "91.44,     100.0,   0.01",   // 100 yards
        "274.32,    300.0,   0.01"    // 300 yards (long driver)
    )
    fun `meters to yards conversion`(meters: Double, expectedYards: Double, tolerance: Double) {
        assertEquals(expectedYards, metersToYards(meters), tolerance)
    }

    @ParameterizedTest(name = "{0}yds = ~{1}m")
    @CsvSource(
        "0.0,     0.0,     0.001",
        "1.0,     0.9144,  0.001",
        "100.0,   91.44,   0.01",
        "300.0,   274.32,  0.01"
    )
    fun `yards to meters conversion`(yards: Double, expectedMeters: Double, tolerance: Double) {
        assertEquals(expectedMeters, yardsToMeters(yards), tolerance)
    }

    @ParameterizedTest(name = "round-trip: {0}m")
    @ValueSource(doubles = [0.0, 1.0, 100.0, 274.32, 500.0])
    fun `round trip meters to yards and back`(meters: Double) {
        val yards = metersToYards(meters)
        val backToMeters = yardsToMeters(yards)
        assertEquals(meters, backToMeters, 0.0001, "Round trip failed for $meters meters")
    }

    @ParameterizedTest(name = "negative input: {0}m")
    @ValueSource(doubles = [-1.0, -100.0])
    fun `negative meters produces negative yards`(meters: Double) {
        assertTrue(metersToYards(meters) < 0)
    }

    @ParameterizedTest(name = "special value: {0}")
    @ValueSource(doubles = [Double.NaN])
    fun `NaN input produces NaN output`(meters: Double) {
        assertTrue(metersToYards(meters).isNaN())
    }

    @ParameterizedTest(name = "infinity: {0}")
    @ValueSource(doubles = [Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY])
    fun `infinity input produces infinity output`(meters: Double) {
        assertTrue(metersToYards(meters).isInfinite())
    }
}
