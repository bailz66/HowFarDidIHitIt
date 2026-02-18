package com.example.howfardidihitit.network

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Weather cache boundary value tests")
class WeatherCacheBoundaryTest {

    private val sampleWeather = WeatherData(
        temperatureCelsius = 22.5,
        weatherCode = 1,
        windSpeedKmh = 15.0,
        windDirectionDegrees = 180
    )

    @Test
    fun `empty cache returns null`() {
        val cache = WeatherCache { 0L }
        assertNull(cache.get())
    }

    @Test
    fun `cache at 0ms age returns data`() {
        var now = 1000L
        val cache = WeatherCache { now }
        cache.put(sampleWeather)
        // Same instant
        assertEquals(sampleWeather, cache.get())
    }

    @Test
    fun `cache at 59m59s (3599000ms) returns data`() {
        var now = 1000L
        val cache = WeatherCache { now }
        cache.put(sampleWeather)
        now = 1000L + 3_599_000L // 59 minutes 59 seconds later
        assertEquals(sampleWeather, cache.get())
    }

    @Test
    fun `cache at exactly 1 hour (3600000ms) is stale`() {
        var now = 1000L
        val cache = WeatherCache { now }
        cache.put(sampleWeather)
        now = 1000L + 3_600_000L // exactly 1 hour
        assertNull(cache.get())
    }

    @Test
    fun `cache at 1 hour plus 1ms is stale`() {
        var now = 1000L
        val cache = WeatherCache { now }
        cache.put(sampleWeather)
        now = 1000L + 3_600_001L
        assertNull(cache.get())
    }

    @Test
    fun `clear removes cached data`() {
        var now = 1000L
        val cache = WeatherCache { now }
        cache.put(sampleWeather)
        cache.clear()
        assertNull(cache.get())
    }

    @Test
    fun `put replaces previous data`() {
        var now = 1000L
        val cache = WeatherCache { now }
        cache.put(sampleWeather)

        val updatedWeather = sampleWeather.copy(temperatureCelsius = 30.0)
        now = 2000L
        cache.put(updatedWeather)

        assertEquals(updatedWeather, cache.get())
    }
}
