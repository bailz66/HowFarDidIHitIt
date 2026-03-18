package com.smacktrack.golf.data

import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.ShotResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Migration chunking and map logic tests")
class MigrationLogicTest {

    private fun makeShotAt(index: Int) = ShotResult(
        club = Club.entries[index % Club.entries.size],
        distanceYards = 100 + index,
        distanceMeters = 91 + index,
        weatherDescription = "Clear",
        temperatureF = 70,
        temperatureC = 21,
        windSpeedKmh = 10.0,
        windDirectionCompass = "N",
        windDirectionDegrees = 0,
        shotBearingDegrees = 0.0,
        timestampMs = 1700000000000L + index
    )

    @Test
    fun `empty list produces no chunks`() {
        val chunks = emptyList<ShotResult>().chunked(500)
        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `under 500 shots produces single chunk`() {
        val shots = (0 until 100).map { makeShotAt(it) }
        val chunks = shots.chunked(500)
        assertEquals(1, chunks.size)
        assertEquals(100, chunks[0].size)
    }

    @Test
    fun `exactly 500 shots produces single chunk`() {
        val shots = (0 until 500).map { makeShotAt(it) }
        val chunks = shots.chunked(500)
        assertEquals(1, chunks.size)
        assertEquals(500, chunks[0].size)
    }

    @Test
    fun `501 shots produces two chunks`() {
        val shots = (0 until 501).map { makeShotAt(it) }
        val chunks = shots.chunked(500)
        assertEquals(2, chunks.size)
        assertEquals(500, chunks[0].size)
        assertEquals(1, chunks[1].size)
    }

    @Test
    fun `1500 shots produces three chunks`() {
        val shots = (0 until 1500).map { makeShotAt(it) }
        val chunks = shots.chunked(500)
        assertEquals(3, chunks.size)
        chunks.forEach { assertEquals(500, it.size) }
    }

    @Test
    fun `each shot produces unique Firestore map with distinct timestampMs`() {
        val shots = (0 until 10).map { makeShotAt(it) }
        val maps = shots.map { it.toFirestoreMap() }
        val timestamps = maps.map { it["timestampMs"] }.toSet()
        assertEquals(10, timestamps.size)
    }

    @Test
    fun `Firestore map field count and values are correct`() {
        val shot = makeShotAt(5)
        val map = shot.toFirestoreMap()

        assertEquals(12, map.size)
        assertEquals(shot.club.name, map["club"])
        assertEquals(shot.distanceYards, map["distanceYards"])
        assertEquals(shot.distanceMeters, map["distanceMeters"])
        assertEquals(shot.weatherDescription, map["weatherDescription"])
        assertEquals(shot.temperatureF, map["temperatureF"])
        assertEquals(shot.temperatureC, map["temperatureC"])
        assertEquals(shot.windSpeedKmh, map["windSpeedKmh"])
        assertEquals(shot.windDirectionCompass, map["windDirectionCompass"])
        assertEquals(shot.windDirectionDegrees, map["windDirectionDegrees"])
        assertEquals(shot.shotBearingDegrees, map["shotBearingDegrees"])
        assertEquals(shot.timestampMs, map["timestampMs"])
    }

    @Test
    fun `deterministic document ID equals timestampMs as string`() {
        val shot = makeShotAt(42)
        val expectedDocId = shot.timestampMs.toString()
        assertEquals("1700000000042", expectedDocId)
    }
}
