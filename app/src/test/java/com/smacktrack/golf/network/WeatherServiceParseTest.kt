package com.smacktrack.golf.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WeatherService JSON parsing tests")
class WeatherServiceParseTest {

    @Test
    fun `parses valid Open-Meteo response`() {
        val json = """
        {
            "current": {
                "temperature_2m": 18.5,
                "weather_code": 2,
                "wind_speed_10m": 14.3,
                "wind_direction_10m": 225
            }
        }
        """.trimIndent()

        val result = WeatherService.parseWeatherJson(json)

        assertNotNull(result)
        assertEquals(18.5, result!!.temperatureCelsius, 0.01)
        assertEquals(2, result.weatherCode)
        assertEquals(14.3, result.windSpeedKmh, 0.01)
        assertEquals(225, result.windDirectionDegrees)
    }

    @Test
    fun `parses zero values`() {
        val json = """
        {
            "current": {
                "temperature_2m": 0.0,
                "weather_code": 0,
                "wind_speed_10m": 0.0,
                "wind_direction_10m": 0
            }
        }
        """.trimIndent()

        val result = WeatherService.parseWeatherJson(json)

        assertNotNull(result)
        assertEquals(0.0, result!!.temperatureCelsius, 0.01)
        assertEquals(0, result.weatherCode)
        assertEquals(0.0, result.windSpeedKmh, 0.01)
        assertEquals(0, result.windDirectionDegrees)
    }

    @Test
    fun `parses negative temperature`() {
        val json = """
        {
            "current": {
                "temperature_2m": -12.7,
                "weather_code": 71,
                "wind_speed_10m": 30.0,
                "wind_direction_10m": 360
            }
        }
        """.trimIndent()

        val result = WeatherService.parseWeatherJson(json)

        assertNotNull(result)
        assertEquals(-12.7, result!!.temperatureCelsius, 0.01)
        assertEquals(71, result.weatherCode)
    }

    @Test
    fun `returns null for empty JSON`() {
        assertNull(WeatherService.parseWeatherJson(""))
    }

    @Test
    fun `returns null for invalid JSON`() {
        assertNull(WeatherService.parseWeatherJson("not json at all"))
    }

    @Test
    fun `returns null for missing current object`() {
        val json = """{ "hourly": {} }"""
        assertNull(WeatherService.parseWeatherJson(json))
    }

    @Test
    fun `returns null for missing fields in current`() {
        val json = """
        {
            "current": {
                "temperature_2m": 20.0
            }
        }
        """.trimIndent()

        assertNull(WeatherService.parseWeatherJson(json))
    }

    @Test
    fun `parses response with extra fields`() {
        val json = """
        {
            "latitude": 37.42,
            "longitude": -122.08,
            "timezone": "America/Los_Angeles",
            "current": {
                "time": "2026-02-21T10:00",
                "interval": 900,
                "temperature_2m": 15.2,
                "weather_code": 3,
                "wind_speed_10m": 8.1,
                "wind_direction_10m": 180,
                "extra_field": 42
            }
        }
        """.trimIndent()

        val result = WeatherService.parseWeatherJson(json)

        assertNotNull(result)
        assertEquals(15.2, result!!.temperatureCelsius, 0.01)
        assertEquals(3, result.weatherCode)
        assertEquals(8.1, result.windSpeedKmh, 0.01)
        assertEquals(180, result.windDirectionDegrees)
    }
}
