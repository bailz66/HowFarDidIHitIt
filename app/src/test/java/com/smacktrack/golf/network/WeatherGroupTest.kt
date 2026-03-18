package com.smacktrack.golf.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("weatherGroup mapping tests")
class WeatherGroupTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(
        "Clear sky, Clear",
        "Mainly clear, Clear",
        "Partly cloudy, Clear",
        "Overcast, Clear",
        "Foggy, Fog",
        "Depositing rime fog, Fog",
        "Light drizzle, Drizzle",
        "Moderate drizzle, Drizzle",
        "Dense drizzle, Drizzle",
        "Freezing drizzle, Drizzle",
        "Slight rain, Rain",
        "Moderate rain, Rain",
        "Heavy rain, Rain",
        "Light freezing rain, Rain",
        "Heavy freezing rain, Rain",
        "Slight snowfall, Snow",
        "Moderate snowfall, Snow",
        "Heavy snowfall, Snow",
        "Snow grains, Snow",
        "Slight rain showers, Showers",
        "Moderate rain showers, Showers",
        "Violent rain showers, Showers",
        "Slight snow showers, Showers",
        "Heavy snow showers, Showers",
        "Thunderstorm, Thunderstorm",
        "Thunderstorm with slight hail, Thunderstorm",
        "Thunderstorm with heavy hail, Thunderstorm"
    )
    fun `weatherGroup maps descriptions correctly`(description: String, expectedGroup: String) {
        assertEquals(expectedGroup, weatherGroup(description))
    }

    @ParameterizedTest(name = "edge case: \"{0}\" -> \"{1}\"")
    @CsvSource(
        "'', ''",
        "Unknown, Unknown"
    )
    fun `edge cases return passthrough`(description: String, expected: String) {
        assertEquals(expected, weatherGroup(description))
    }
}
