package com.example.howfardidihitit.location

import com.example.howfardidihitit.domain.GpsCoordinate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("GPS calibration boundary value tests")
class GpsCalibrationBoundaryTest {

    @Test
    fun `min samples (3) returns calibrated coordinate`() {
        val samples = listOf(
            GpsCoordinate(33.749, -84.388),
            GpsCoordinate(33.7491, -84.3881),
            GpsCoordinate(33.7489, -84.3879)
        )
        val result = calibrate(samples)
        assertNotNull(result)
        assertEquals(33.749, result!!.lat, 0.001)
        assertEquals(-84.388, result.lon, 0.001)
    }

    @Test
    fun `below min samples (2) returns null`() {
        val samples = listOf(
            GpsCoordinate(33.749, -84.388),
            GpsCoordinate(33.7491, -84.3881)
        )
        assertNull(calibrate(samples))
    }

    @Test
    fun `single sample returns null`() {
        assertNull(calibrate(listOf(GpsCoordinate(33.749, -84.388))))
    }

    @Test
    fun `empty list returns null`() {
        assertNull(calibrate(emptyList()))
    }

    @Test
    fun `tight cluster plus 1 spike rejects outlier`() {
        val samples = listOf(
            GpsCoordinate(33.7490, -84.3880),
            GpsCoordinate(33.7491, -84.3881),
            GpsCoordinate(33.7489, -84.3879),
            GpsCoordinate(33.7490, -84.3880),
            GpsCoordinate(34.0000, -84.0000) // spike ~28km away
        )
        val result = calibrate(samples)
        assertNotNull(result)
        // Result should be close to the cluster center, not pulled by the spike
        assertEquals(33.749, result!!.lat, 0.001)
        assertEquals(-84.388, result.lon, 0.001)
    }

    @Test
    fun `tight cluster plus 2 spikes rejects outliers`() {
        val samples = listOf(
            GpsCoordinate(33.7490, -84.3880),
            GpsCoordinate(33.7491, -84.3881),
            GpsCoordinate(33.7489, -84.3879),
            GpsCoordinate(34.0000, -84.0000), // spike
            GpsCoordinate(33.0000, -85.0000)  // spike
        )
        val result = calibrate(samples)
        assertNotNull(result)
        assertEquals(33.749, result!!.lat, 0.001)
    }

    @Test
    fun `all identical samples returns that coordinate`() {
        val coord = GpsCoordinate(33.749, -84.388)
        val samples = List(5) { coord }
        val result = calibrate(samples)
        assertNotNull(result)
        assertEquals(coord.lat, result!!.lat, 0.0001)
        assertEquals(coord.lon, result.lon, 0.0001)
    }

    @Test
    fun `extreme coordinates near poles`() {
        val samples = listOf(
            GpsCoordinate(89.9999, 0.0),
            GpsCoordinate(89.9999, 0.00001),
            GpsCoordinate(89.9999, -0.00001)
        )
        val result = calibrate(samples)
        assertNotNull(result)
        assertTrue(result!!.lat > 89.99)
    }

    @Test
    fun `coordinates crossing 0 degrees longitude`() {
        val samples = listOf(
            GpsCoordinate(51.5, -0.001),
            GpsCoordinate(51.5, 0.001),
            GpsCoordinate(51.5, 0.0)
        )
        val result = calibrate(samples)
        assertNotNull(result)
        assertEquals(0.0, result!!.lon, 0.01)
    }

    @Test
    fun `10 samples max typical use case`() {
        val base = GpsCoordinate(33.749, -84.388)
        val samples = (0 until 10).map {
            GpsCoordinate(base.lat + (it - 5) * 0.00001, base.lon + (it - 5) * 0.00001)
        }
        val result = calibrate(samples)
        assertNotNull(result)
        assertEquals(base.lat, result!!.lat, 0.001)
        assertEquals(base.lon, result.lon, 0.001)
    }

    @Test
    fun `all outliers (widely scattered) may return null`() {
        // 3 points far apart — each could be considered an outlier relative to median
        val samples = listOf(
            GpsCoordinate(0.0, 0.0),
            GpsCoordinate(45.0, 90.0),
            GpsCoordinate(-45.0, -90.0)
        )
        // Result depends on implementation, but should either return null or a valid coordinate
        val result = calibrate(samples)
        // If result is non-null, it should be a valid coordinate
        result?.let {
            assertTrue(it.lat in -90.0..90.0)
            assertTrue(it.lon in -180.0..180.0)
        }
    }

    @Test
    fun `negative latitude samples calibrate correctly`() {
        val samples = listOf(
            GpsCoordinate(-33.868, 151.207),
            GpsCoordinate(-33.867, 151.208),
            GpsCoordinate(-33.869, 151.206)
        )
        val result = calibrate(samples)
        assertNotNull(result)
        assertEquals(-33.868, result!!.lat, 0.002)
    }
}
