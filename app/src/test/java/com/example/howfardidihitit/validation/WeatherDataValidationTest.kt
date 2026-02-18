package com.example.howfardidihitit.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("Weather data validation tests")
class WeatherDataValidationTest {

    @Test
    fun `all null weather fields are valid (no weather recorded)`() {
        val result = validateWeather(null, null, null, null)
        assertTrue(result.isValid)
    }

    @Test
    fun `all present valid weather passes`() {
        val result = validateWeather(
            temperatureCelsius = 22.5,
            windSpeedMph = 10.0,
            windDirectionDegrees = 180,
            weatherCode = 1
        )
        assertTrue(result.isValid)
    }

    @Test
    fun `partial weather fields fail (temp only)`() {
        val result = validateWeather(
            temperatureCelsius = 22.5,
            windSpeedMph = null,
            windDirectionDegrees = null,
            weatherCode = null
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("all present or all absent") })
    }

    @Test
    fun `partial weather fields fail (missing weather code)`() {
        val result = validateWeather(
            temperatureCelsius = 22.5,
            windSpeedMph = 10.0,
            windDirectionDegrees = 180,
            weatherCode = null
        )
        assertFalse(result.isValid)
    }

    // --- Temperature extremes ---

    @ParameterizedTest(name = "Valid temp: {0}°C")
    @CsvSource(
        "-89.2",   // coldest recorded
        "-89.1",   // just above coldest
        "0.0",
        "22.5",
        "56.6",    // just below hottest
        "56.7"     // hottest recorded
    )
    fun `temperatures within Earth extremes are valid`(celsius: Double) {
        val result = validateWeather(celsius, 10.0, 180, 1)
        assertTrue(result.isValid, "Expected valid for ${celsius}°C: ${result.errors}")
    }

    @ParameterizedTest(name = "Invalid temp: {0}°C")
    @CsvSource(
        "-89.3",   // below coldest
        "56.8",    // above hottest
        "-100.0",
        "100.0"
    )
    fun `temperatures outside Earth extremes are invalid`(celsius: Double) {
        val result = validateWeather(celsius, 10.0, 180, 1)
        assertFalse(result.isValid)
    }

    // --- Wind speed ---

    @Test
    fun `wind speed 0 is valid`() {
        assertTrue(validateWeather(22.0, 0.0, 180, 1).isValid)
    }

    @Test
    fun `wind speed 253 mph is valid (record)`() {
        assertTrue(validateWeather(22.0, 253.0, 180, 1).isValid)
    }

    @Test
    fun `negative wind speed is invalid`() {
        assertFalse(validateWeather(22.0, -1.0, 180, 1).isValid)
    }

    @Test
    fun `wind speed above 253 mph is invalid`() {
        assertFalse(validateWeather(22.0, 254.0, 180, 1).isValid)
    }

    // --- Wind direction ---

    @Test
    fun `wind direction 0 is valid`() {
        assertTrue(validateWeather(22.0, 10.0, 0, 1).isValid)
    }

    @Test
    fun `wind direction 359 is valid`() {
        assertTrue(validateWeather(22.0, 10.0, 359, 1).isValid)
    }

    @Test
    fun `wind direction -1 is invalid`() {
        assertFalse(validateWeather(22.0, 10.0, -1, 1).isValid)
    }

    @Test
    fun `wind direction 360 is invalid`() {
        assertFalse(validateWeather(22.0, 10.0, 360, 1).isValid)
    }
}
