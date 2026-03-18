package com.smacktrack.golf.location

import com.smacktrack.golf.domain.GpsCoordinate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("bearingDegrees tests")
class BearingTest {

    @Test
    fun `due north bearing is approximately 0`() {
        val start = GpsCoordinate(33.749, -84.388)
        val end = GpsCoordinate(34.749, -84.388)
        val bearing = bearingDegrees(start, end)
        assertEquals(0.0, bearing, 1.0)
    }

    @Test
    fun `due south bearing is approximately 180`() {
        val start = GpsCoordinate(34.749, -84.388)
        val end = GpsCoordinate(33.749, -84.388)
        val bearing = bearingDegrees(start, end)
        assertEquals(180.0, bearing, 1.0)
    }

    @Test
    fun `due east bearing is approximately 90`() {
        val start = GpsCoordinate(0.0, 0.0)
        val end = GpsCoordinate(0.0, 1.0)
        val bearing = bearingDegrees(start, end)
        assertEquals(90.0, bearing, 1.0)
    }

    @Test
    fun `due west bearing is approximately 270`() {
        val start = GpsCoordinate(0.0, 1.0)
        val end = GpsCoordinate(0.0, 0.0)
        val bearing = bearingDegrees(start, end)
        assertEquals(270.0, bearing, 1.0)
    }

    @Test
    fun `same point returns 0`() {
        val point = GpsCoordinate(33.749, -84.388)
        val bearing = bearingDegrees(point, point)
        assertEquals(0.0, bearing, 0.001)
    }

    @Test
    fun `northeast bearing is approximately 45`() {
        val start = GpsCoordinate(0.0, 0.0)
        val end = GpsCoordinate(1.0, 1.0)
        val bearing = bearingDegrees(start, end)
        assertTrue(bearing in 40.0..50.0, "Expected ~45 but got $bearing")
    }

    @Test
    fun `bearing is always in 0 to 360 range`() {
        val cases = listOf(
            GpsCoordinate(0.0, 0.0) to GpsCoordinate(1.0, 0.0),
            GpsCoordinate(0.0, 0.0) to GpsCoordinate(-1.0, 0.0),
            GpsCoordinate(0.0, 0.0) to GpsCoordinate(0.0, 1.0),
            GpsCoordinate(0.0, 0.0) to GpsCoordinate(0.0, -1.0),
            GpsCoordinate(89.0, 0.0) to GpsCoordinate(89.0, 180.0)
        )
        for ((start, end) in cases) {
            val bearing = bearingDegrees(start, end)
            assertTrue(bearing in 0.0..360.0, "Bearing $bearing out of range for $start -> $end")
        }
    }

    @Test
    fun `bearing across international date line`() {
        val start = GpsCoordinate(0.0, 179.5)
        val end = GpsCoordinate(0.0, -179.5)
        val bearing = bearingDegrees(start, end)
        // Crossing date line eastward should give ~90
        assertEquals(90.0, bearing, 5.0)
    }
}
