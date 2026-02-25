package com.smacktrack.golf.data

import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.TemperatureUnit
import com.smacktrack.golf.ui.Trajectory
import com.smacktrack.golf.ui.WindUnit
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Shot serialization tests")
class ShotSerializationTest {

    private val sampleShot = ShotResult(
        club = Club.SEVEN_IRON,
        distanceYards = 150,
        distanceMeters = 137,
        weatherDescription = "Clear sky",
        temperatureF = 72,
        temperatureC = 22,
        windSpeedKmh = 12.5,
        windDirectionCompass = "NW",
        windDirectionDegrees = 315,
        shotBearingDegrees = 45.0,
        timestampMs = 1700000000000L
    )

    @Test
    fun `JSON round-trip preserves all fields`() {
        val json = sampleShot.toJson()
        val restored = json.toShotResult()

        assertEquals(sampleShot.club, restored.club)
        assertEquals(sampleShot.distanceYards, restored.distanceYards)
        assertEquals(sampleShot.distanceMeters, restored.distanceMeters)
        assertEquals(sampleShot.weatherDescription, restored.weatherDescription)
        assertEquals(sampleShot.temperatureF, restored.temperatureF)
        assertEquals(sampleShot.temperatureC, restored.temperatureC)
        assertEquals(sampleShot.windSpeedKmh, restored.windSpeedKmh, 0.001)
        assertEquals(sampleShot.windDirectionCompass, restored.windDirectionCompass)
        assertEquals(sampleShot.windDirectionDegrees, restored.windDirectionDegrees)
        assertEquals(sampleShot.shotBearingDegrees, restored.shotBearingDegrees, 0.001)
        assertEquals(sampleShot.timestampMs, restored.timestampMs)
    }

    @Test
    fun `Firestore map produces correct keys and values`() {
        val map = sampleShot.toFirestoreMap()

        assertEquals(12, map.size)
        assertEquals("SEVEN_IRON", map["club"])
        assertEquals(150, map["distanceYards"])
        assertEquals(137, map["distanceMeters"])
        assertEquals("Clear sky", map["weatherDescription"])
        assertEquals(72, map["temperatureF"])
        assertEquals(22, map["temperatureC"])
        assertEquals(12.5, map["windSpeedKmh"])
        assertEquals("NW", map["windDirectionCompass"])
        assertEquals(315, map["windDirectionDegrees"])
        assertEquals(45.0, map["shotBearingDegrees"])
        assertEquals(1700000000000L, map["timestampMs"])
    }

    @Test
    fun `missing optional fields get safe defaults`() {
        val minimal = JSONObject().apply {
            put("club", "DRIVER")
            put("distanceYards", 200)
            put("distanceMeters", 183)
            put("weatherDescription", "Sunny")
            put("temperatureF", 80)
            put("temperatureC", 27)
            put("windSpeedKmh", 5.0)
            put("windDirectionCompass", "N")
            // windDirectionDegrees, shotBearingDegrees, timestampMs omitted
        }
        val result = minimal.toShotResult()

        assertEquals(0, result.windDirectionDegrees)
        assertEquals(0.0, result.shotBearingDegrees, 0.001)
        // timestampMs defaults to System.currentTimeMillis() — just verify it's > 0
        assertTrue(result.timestampMs > 0)
    }

    @Test
    fun `unknown club name falls back to DRIVER`() {
        val json = sampleShot.toJson()
        json.put("club", "NONEXISTENT_CLUB")
        val result = json.toShotResult()
        assertEquals(Club.DRIVER, result.club)
    }

    @Test
    fun `zero and default values handled correctly`() {
        val zeroShot = ShotResult(
            club = Club.DRIVER,
            distanceYards = 0,
            distanceMeters = 0,
            weatherDescription = "",
            temperatureF = 0,
            temperatureC = 0,
            windSpeedKmh = 0.0,
            windDirectionCompass = "",
            windDirectionDegrees = 0,
            shotBearingDegrees = 0.0,
            timestampMs = 0L
        )
        val json = zeroShot.toJson()
        val restored = json.toShotResult()

        assertEquals(0, restored.distanceYards)
        assertEquals(0, restored.distanceMeters)
        assertEquals("", restored.weatherDescription)
        assertEquals(0.0, restored.windSpeedKmh, 0.001)
        assertEquals(0L, restored.timestampMs)
    }

    @Test
    fun `AppSettings toFirestoreMap produces correct keys`() {
        val settings = AppSettings(
            distanceUnit = DistanceUnit.METERS,
            windUnit = WindUnit.KMH,
            temperatureUnit = TemperatureUnit.CELSIUS,
            trajectory = Trajectory.HIGH,
            enabledClubs = setOf(Club.DRIVER, Club.SEVEN_IRON)
        )
        val map = settings.toFirestoreMap()

        assertEquals(6, map.size)
        assertEquals("METERS", map["distanceUnit"])
        assertEquals("KMH", map["windUnit"])
        assertEquals("CELSIUS", map["temperatureUnit"])
        assertEquals("HIGH", map["trajectory"])
        @Suppress("UNCHECKED_CAST")
        val clubs = map["enabledClubs"] as List<String>
        assertTrue(clubs.contains("DRIVER"))
        assertTrue(clubs.contains("SEVEN_IRON"))
        assertEquals(2, clubs.size)
    }

    @Test
    fun `enumValueOfOrNull returns null for invalid name`() {
        assertNull(enumValueOfOrNull<Club>("NOT_A_CLUB"))
        assertNull(enumValueOfOrNull<DistanceUnit>("FURLONGS"))
    }

    @Test
    fun `schemaVersion present in Firestore map`() {
        val map = sampleShot.toFirestoreMap()
        assertEquals(1, map["schemaVersion"])
    }

    @Test
    fun `schemaVersion present in AppSettings Firestore map`() {
        val map = AppSettings().toFirestoreMap()
        assertEquals(1, map["schemaVersion"])
    }

    @Test
    fun `old JSON without schemaVersion still parses`() {
        val json = JSONObject().apply {
            put("club", "DRIVER")
            put("distanceYards", 250)
            put("distanceMeters", 229)
            put("weatherDescription", "Sunny")
            put("temperatureF", 75)
            put("temperatureC", 24)
            put("windSpeedKmh", 8.0)
            put("windDirectionCompass", "S")
            // no schemaVersion key
        }
        val result = json.toShotResult()
        assertEquals(Club.DRIVER, result.club)
        assertEquals(250, result.distanceYards)
    }

    @Test
    fun `distinctBy timestampMs removes duplicates`() {
        val shot1 = sampleShot
        val shot2 = sampleShot.copy(distanceYards = 200) // same timestampMs
        val shot3 = sampleShot.copy(timestampMs = 1700000000001L) // different
        val list = listOf(shot1, shot2, shot3)
        val deduped = list.distinctBy { it.timestampMs }
        assertEquals(2, deduped.size)
        assertEquals(1700000000000L, deduped[0].timestampMs)
        assertEquals(1700000000001L, deduped[1].timestampMs)
    }
}
