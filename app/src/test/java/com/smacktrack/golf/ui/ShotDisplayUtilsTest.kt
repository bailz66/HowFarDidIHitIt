package com.smacktrack.golf.ui

import com.smacktrack.golf.domain.Club
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("ShotDisplayUtils formatting and percentile tests")
class ShotDisplayUtilsTest {

    private fun shot(
        club: Club = Club.SEVEN_IRON,
        distanceYards: Int = 150,
        distanceMeters: Int = 137,
        windSpeedKmh: Double = 16.0,
        temperatureF: Int = 72,
        temperatureC: Int = 22,
        timestampMs: Long = 1700000000000L
    ) = ShotResult(
        club = club,
        distanceYards = distanceYards,
        distanceMeters = distanceMeters,
        weatherDescription = "Clear sky",
        temperatureF = temperatureF,
        temperatureC = temperatureC,
        windSpeedKmh = windSpeedKmh,
        windDirectionCompass = "NW",
        windDirectionDegrees = 315,
        shotBearingDegrees = 45.0,
        timestampMs = timestampMs
    )

    // ── primaryDistance ──────────────────────────────────────────────────────

    @Test
    @DisplayName("primaryDistance returns yards in YARDS mode")
    fun primaryDistanceYards() {
        val s = shot(distanceYards = 200, distanceMeters = 183)
        assertEquals(200, s.primaryDistance(DistanceUnit.YARDS))
    }

    @Test
    @DisplayName("primaryDistance returns meters in METERS mode")
    fun primaryDistanceMeters() {
        val s = shot(distanceYards = 200, distanceMeters = 183)
        assertEquals(183, s.primaryDistance(DistanceUnit.METERS))
    }

    // ── primaryUnitLabel ────────────────────────────────────────────────────

    @Test
    @DisplayName("primaryUnitLabel returns YARDS/METERS")
    fun primaryUnitLabels() {
        val s = shot()
        assertEquals("YARDS", s.primaryUnitLabel(DistanceUnit.YARDS))
        assertEquals("METERS", s.primaryUnitLabel(DistanceUnit.METERS))
    }

    // ── secondaryDistance ────────────────────────────────────────────────────

    @Test
    @DisplayName("secondaryDistance returns opposite unit with suffix")
    fun secondaryDistanceFormats() {
        val s = shot(distanceYards = 200, distanceMeters = 183)
        assertEquals("183m", s.secondaryDistance(DistanceUnit.YARDS))
        assertEquals("200yd", s.secondaryDistance(DistanceUnit.METERS))
    }

    // ── shortUnitLabel ──────────────────────────────────────────────────────

    @Test
    @DisplayName("shortUnitLabel returns yds/m")
    fun shortUnitLabels() {
        val s = shot()
        assertEquals("yds", s.shortUnitLabel(DistanceUnit.YARDS))
        assertEquals("m", s.shortUnitLabel(DistanceUnit.METERS))
    }

    // ── formatWindSpeed ─────────────────────────────────────────────────────

    @Test
    @DisplayName("formatWindSpeed in km/h mode")
    fun formatWindSpeedKmh() {
        val s = shot(windSpeedKmh = 16.0)
        assertEquals("16 km/h", s.formatWindSpeed(WindUnit.KMH))
    }

    @Test
    @DisplayName("formatWindSpeed in mph mode")
    fun formatWindSpeedMph() {
        val s = shot(windSpeedKmh = 16.0)
        // 16 * 0.621371 = 9.94 → 9 mph
        assertEquals("9 mph", s.formatWindSpeed(WindUnit.MPH))
    }

    // ── formatTemperature ───────────────────────────────────────────────────

    @Test
    @DisplayName("formatTemperature in Fahrenheit mode")
    fun formatTemperatureFahrenheit() {
        val s = shot(temperatureF = 72, temperatureC = 22)
        assertEquals("72\u00B0F", s.formatTemperature(TemperatureUnit.FAHRENHEIT))
    }

    @Test
    @DisplayName("formatTemperature in Celsius mode")
    fun formatTemperatureCelsius() {
        val s = shot(temperatureF = 72, temperatureC = 22)
        assertEquals("22\u00B0C", s.formatTemperature(TemperatureUnit.CELSIUS))
    }

    // ── percentileAmongClub ─────────────────────────────────────────────────

    @Test
    @DisplayName("percentileAmongClub returns null with fewer than 5 prior shots")
    fun percentileNullWithFewShots() {
        val current = shot(distanceYards = 150, timestampMs = 100)
        val history = (1..4).map { shot(distanceYards = 100 + it * 10, timestampMs = it.toLong()) }
        assertNull(current.percentileAmongClub(history, DistanceUnit.YARDS))
    }

    @Test
    @DisplayName("percentileAmongClub filters by same club only")
    fun percentileFiltersByClub() {
        val current = shot(club = Club.DRIVER, distanceYards = 250, timestampMs = 100)
        // 5 prior SEVEN_IRON shots, 0 prior DRIVER shots
        val history = (1..5).map { shot(club = Club.SEVEN_IRON, distanceYards = 100, timestampMs = it.toLong()) }
        assertNull(current.percentileAmongClub(history, DistanceUnit.YARDS))
    }

    @Test
    @DisplayName("percentileAmongClub at 0% — worst shot")
    fun percentileZero() {
        val current = shot(distanceYards = 50, timestampMs = 100)
        val history = (1..5).map { shot(distanceYards = 100 + it * 10, timestampMs = it.toLong()) }
        val p = current.percentileAmongClub(history, DistanceUnit.YARDS)!!
        assertEquals(0f, p)
    }

    @Test
    @DisplayName("percentileAmongClub at 100% — best shot")
    fun percentileHundred() {
        val current = shot(distanceYards = 999, timestampMs = 100)
        val history = (1..5).map { shot(distanceYards = 100 + it * 10, timestampMs = it.toLong()) }
        val p = current.percentileAmongClub(history, DistanceUnit.YARDS)!!
        assertEquals(100f, p)
    }

    @Test
    @DisplayName("percentileAmongClub at 50% — median shot")
    fun percentileFifty() {
        val current = shot(distanceYards = 130, timestampMs = 100)
        // Prior: 110, 120, 130, 140, 150, 160 (6 shots)
        val history = (1..6).map { shot(distanceYards = 100 + it * 10, timestampMs = it.toLong()) }
        val p = current.percentileAmongClub(history, DistanceUnit.YARDS)!!
        // Beats 110, 120, 130 (>=) = 3 out of 6 = 50%
        assertEquals(50f, p)
    }

    @Test
    @DisplayName("percentileAmongClub at 80% threshold")
    fun percentileEighty() {
        val current = shot(distanceYards = 148, timestampMs = 100)
        // Prior: 110, 120, 130, 140, 150 (5 shots)
        val history = (1..5).map { shot(distanceYards = 100 + it * 10, timestampMs = it.toLong()) }
        val p = current.percentileAmongClub(history, DistanceUnit.YARDS)!!
        // Beats 110, 120, 130, 140 = 4 out of 5 = 80%
        assertEquals(80f, p)
    }

    @Test
    @DisplayName("percentileAmongClub at 95%+ — top of range")
    fun percentileNinetyFive() {
        val current = shot(distanceYards = 200, timestampMs = 100)
        // Prior: 20 shots from 100 to 190
        val history = (1..20).map { shot(distanceYards = 90 + it * 5, timestampMs = it.toLong()) }
        val p = current.percentileAmongClub(history, DistanceUnit.YARDS)!!
        // Beats all 20 = 100%
        assertEquals(100f, p)
    }

    // ── windStrengthLabel ───────────────────────────────────────────────────

    @ParameterizedTest
    @DisplayName("windStrengthLabel at each threshold boundary")
    @CsvSource(
        "0.0, None",
        "5.9, None",
        "6.0, Very Light",
        "12.9, Very Light",
        "13.0, Light",
        "19.9, Light",
        "20.0, Medium",
        "35.9, Medium",
        "36.0, Strong",
        "49.9, Strong",
        "50.0, Very Strong",
        "70.9, Very Strong",
        "71.0, Why are you even out here?!",
        "100.0, Why are you even out here?!"
    )
    fun windStrengthBoundaries(speedKmh: Double, expected: String) {
        assertEquals(expected, windStrengthLabel(speedKmh))
    }
}
