package com.example.howfardidihitit.validation

import com.example.howfardidihitit.domain.GpsCoordinate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Test

@DisplayName("GPS coordinate validation tests")
class GpsCoordinateValidationTest {

    @ParameterizedTest(name = "Valid: lat={0}, lon={1}")
    @CsvSource(
        "0.0,      0.0",      // origin
        "90.0,     180.0",    // max valid
        "-90.0,   -180.0",    // min valid
        "33.749,  -84.388",   // Atlanta
        "-33.868,  151.207",  // Sydney
        "90.0,     0.0",      // North Pole
        "-90.0,    0.0",      // South Pole
        "0.0,      180.0",    // Date Line
        "0.0,     -180.0"     // Date Line (west)
    )
    fun `valid coordinates pass validation`(lat: Double, lon: Double) {
        val result = validateCoordinate(GpsCoordinate(lat, lon))
        assertTrue(result.isValid, "Expected valid for ($lat, $lon): ${result.errors}")
    }

    @ParameterizedTest(name = "Invalid: lat={0}, lon={1}")
    @CsvSource(
        "90.1,     0.0",      // lat too high
        "-90.1,    0.0",      // lat too low
        "0.0,      180.1",    // lon too high
        "0.0,     -180.1",    // lon too low
        "91.0,     0.0",      // lat way too high
        "0.0,      181.0"     // lon way too high
    )
    fun `out-of-range coordinates fail validation`(lat: Double, lon: Double) {
        val result = validateCoordinate(GpsCoordinate(lat, lon))
        assertFalse(result.isValid, "Expected invalid for ($lat, $lon)")
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `NaN latitude is rejected`() {
        val result = validateCoordinate(GpsCoordinate(Double.NaN, 0.0))
        assertFalse(result.isValid)
    }

    @Test
    fun `NaN longitude is rejected`() {
        val result = validateCoordinate(GpsCoordinate(0.0, Double.NaN))
        assertFalse(result.isValid)
    }

    @Test
    fun `positive infinity latitude is rejected`() {
        val result = validateCoordinate(GpsCoordinate(Double.POSITIVE_INFINITY, 0.0))
        assertFalse(result.isValid)
    }

    @Test
    fun `negative infinity longitude is rejected`() {
        val result = validateCoordinate(GpsCoordinate(0.0, Double.NEGATIVE_INFINITY))
        assertFalse(result.isValid)
    }

    @Test
    fun `both NaN produces two errors`() {
        val result = validateCoordinate(GpsCoordinate(Double.NaN, Double.NaN))
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
    }
}
