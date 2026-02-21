package com.smacktrack.golf.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches current weather conditions from the Open-Meteo API.
 *
 * Uses [HttpURLConnection] with connect/read timeouts to prevent indefinite hangs.
 * All network I/O runs on [Dispatchers.IO]. Failures return `null` silently —
 * weather is a nice-to-have and must never block shot tracking.
 */
object WeatherService {

    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 10_000

    /**
     * Fetches current weather for the given GPS coordinates.
     *
     * @param lat Latitude in degrees.
     * @param lon Longitude in degrees.
     * @return Parsed [WeatherData] or `null` if the request fails or the response is malformed.
     */
    suspend fun fetchWeather(lat: Double, lon: Double): WeatherData? =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                parseWeatherJson(json)
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Parses a raw JSON response from the Open-Meteo API into [WeatherData].
     *
     * @param json Raw JSON string from the API.
     * @return Parsed [WeatherData] or `null` if the JSON structure is unexpected.
     */
    internal fun parseWeatherJson(json: String): WeatherData? =
        try {
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            WeatherData(
                temperatureCelsius = current.getDouble("temperature_2m"),
                weatherCode = current.getInt("weather_code"),
                windSpeedKmh = current.getDouble("wind_speed_10m"),
                windDirectionDegrees = current.getInt("wind_direction_10m")
            )
        } catch (_: Exception) {
            null
        }
}
