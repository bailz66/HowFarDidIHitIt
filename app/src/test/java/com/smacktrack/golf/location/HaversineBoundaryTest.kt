package com.smacktrack.golf.location

import com.smacktrack.golf.domain.GpsCoordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("Haversine boundary value tests")
class HaversineBoundaryTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        // description, lat1, lon1, lat2, lon2, expectedMeters, toleranceMeters
        "Same point (0m),                  33.749,  -84.388,  33.749,   -84.388,       0.0,     0.1",
        "Short chip (~5yds ≈ 4.57m),       33.7490, -84.3880, 33.74904, -84.3880,      4.4,     1.0",
        "Typical PW (~108yds ≈ 99m),       33.7490, -84.3880, 33.7499,  -84.3880,    100.0,     5.0",
        "Long driver (~300yds ≈ 274m),     33.7490, -84.3880, 33.7515,  -84.3880,    278.0,    10.0",
        "North Pole to 89N,                90.0,      0.0,    89.0,       0.0,     111195.0,   100.0",
        "South Pole to 89S,               -90.0,      0.0,   -89.0,       0.0,     111195.0,   100.0",
        "Equator 1 degree lon,              0.0,      0.0,     0.0,        1.0,     111195.0,   100.0",
        "International Date Line crossing,  0.0,    179.5,     0.0,      -179.5,     111195.0,   200.0",
        "Antipodal points,                  0.0,      0.0,     0.0,      180.0,   20015087.0,  1000.0",
        "Negative latitude,               -33.868,  151.207, -33.856,   151.215,     1500.0,   100.0",
        "Prime Meridian crossing,          51.5,     -0.1,    51.5,        0.1,      13800.0,   200.0",
        "Atlanta to Augusta (~222km),      33.749,  -84.388,  33.474,   -82.010,    222320.0,  1500.0"
    )
    fun `haversine distance matches expected value`(
        description: String,
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        expectedMeters: Double,
        toleranceMeters: Double
    ) {
        val start = GpsCoordinate(lat1, lon1)
        val end = GpsCoordinate(lat2, lon2)
        val actual = haversineMeters(start, end)
        assertEquals(expectedMeters, actual, toleranceMeters, "$description: expected ~${expectedMeters}m, got ${actual}m")
    }

    @ParameterizedTest(name = "metersToYards({0}m) = ~{1}yds")
    @CsvSource(
        "0.0,     0.0,    0.001",
        "0.9144,  1.0,    0.001",
        "91.44,   100.0,  0.01",
        "274.32,  300.0,  0.01"
    )
    fun `metersToYards conversion`(meters: Double, expectedYards: Double, tolerance: Double) {
        assertEquals(expectedYards, metersToYards(meters), tolerance)
    }
}
