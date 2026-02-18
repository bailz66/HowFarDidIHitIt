package com.example.howfardidihitit.network

/**
 * Maps WMO weather code to a human-readable label.
 * See https://open-meteo.com/en/docs for the full WMO code table.
 */
fun wmoCodeToLabel(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45 -> "Foggy"
    48 -> "Depositing rime fog"
    51 -> "Light drizzle"
    53 -> "Moderate drizzle"
    55 -> "Dense drizzle"
    56 -> "Light freezing drizzle"
    57 -> "Dense freezing drizzle"
    61 -> "Slight rain"
    63 -> "Moderate rain"
    65 -> "Heavy rain"
    66 -> "Light freezing rain"
    67 -> "Heavy freezing rain"
    71 -> "Slight snowfall"
    73 -> "Moderate snowfall"
    75 -> "Heavy snowfall"
    77 -> "Snow grains"
    80 -> "Slight rain showers"
    81 -> "Moderate rain showers"
    82 -> "Violent rain showers"
    85 -> "Slight snow showers"
    86 -> "Heavy snow showers"
    95 -> "Thunderstorm"
    96 -> "Thunderstorm with slight hail"
    99 -> "Thunderstorm with heavy hail"
    else -> "Unknown"
}

/**
 * Converts wind direction in degrees to a compass label (N, NE, E, etc.).
 *
 * @param degrees Wind direction in degrees [0, 360). Values outside
 *   this range throw [IllegalArgumentException].
 * @return One of: N, NE, E, SE, S, SW, W, NW
 */
fun degreesToCompass(degrees: Int): String {
    require(degrees in 0..360) { "Degrees must be 0-360, got $degrees" }
    val normalized = degrees % 360
    return when {
        normalized < 23  -> "N"
        normalized < 68  -> "NE"
        normalized < 113 -> "E"
        normalized < 158 -> "SE"
        normalized < 203 -> "S"
        normalized < 248 -> "SW"
        normalized < 293 -> "W"
        normalized < 338 -> "NW"
        else             -> "N"
    }
}

/**
 * Converts Celsius to Fahrenheit.
 */
fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0
