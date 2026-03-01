package com.smacktrack.golf.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Weather mapping boundary value tests")
class WeatherBoundaryTest {

    // --- Temperature conversion ---

    @ParameterizedTest(name = "{0}°C = {1}°F")
    @CsvSource(
        // celsius, expectedFahrenheit, tolerance
        "-273.15,  -459.67,  0.01",   // absolute zero
        "0.0,       32.0,    0.01",   // freezing point
        "100.0,    212.0,    0.01",   // boiling point
        "-40.0,    -40.0,    0.01",   // crossover point
        "37.0,      98.6,    0.01",   // body temperature
        "-89.2,   -128.56,   0.01",   // coldest recorded on Earth
        "56.7,     134.06,   0.01"    // hottest recorded on Earth
    )
    fun `celsius to fahrenheit conversion`(celsius: Double, expectedF: Double, tolerance: Double) {
        assertEquals(expectedF, celsiusToFahrenheit(celsius), tolerance)
    }

    // --- Wind direction ---

    @ParameterizedTest(name = "{0}° = {1}")
    @CsvSource(
        "0,   N",
        "22,  N",
        "23,  NE",
        "45,  NE",
        "67,  NE",
        "68,  E",
        "90,  E",
        "112, E",
        "113, SE",
        "135, SE",
        "157, SE",
        "158, S",
        "180, S",
        "202, S",
        "203, SW",
        "225, SW",
        "247, SW",
        "248, W",
        "270, W",
        "292, W",
        "293, NW",
        "315, NW",
        "337, NW",
        "338, N",
        "359, N",
        "360, N"
    )
    fun `degrees to compass direction`(degrees: Int, expectedDirection: String) {
        assertEquals(expectedDirection, degreesToCompass(degrees))
    }

    @ParameterizedTest(name = "Out-of-range degrees normalized: {0} = {1}")
    @CsvSource(
        "-1,   N",
        "-180, S",
        "361,  N",
        "1000, W"
    )
    fun `out-of-range degrees are normalized`(degrees: Int, expectedDirection: String) {
        assertEquals(expectedDirection, degreesToCompass(degrees))
    }

    // --- WMO code mapping ---

    @ParameterizedTest(name = "WMO {0} = {1}")
    @CsvSource(
        "0,  Clear sky",
        "1,  Mainly clear",
        "2,  Partly cloudy",
        "3,  Overcast",
        "95, Thunderstorm",
        "99, Thunderstorm with heavy hail"
    )
    fun `known WMO codes map to expected labels`(code: Int, expectedLabel: String) {
        assertEquals(expectedLabel, wmoCodeToLabel(code))
    }

    @ParameterizedTest(name = "Unknown WMO code: {0}")
    @ValueSource(ints = [-1, 4, 100, Int.MAX_VALUE])
    fun `unknown WMO codes map to Unknown`(code: Int) {
        assertEquals("Unknown", wmoCodeToLabel(code))
    }

    @Test
    fun `all valid WMO codes return non-empty labels`() {
        val validCodes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        for (code in validCodes) {
            val label = wmoCodeToLabel(code)
            assert(label.isNotEmpty()) { "WMO code $code returned empty label" }
            assert(label != "Unknown") { "WMO code $code returned Unknown but should be known" }
        }
    }
}
