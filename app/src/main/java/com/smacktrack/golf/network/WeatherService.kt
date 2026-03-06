package com.smacktrack.golf.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
            // Round to 2 decimal places (~1.1 km precision) for privacy
            // Force Locale.US to ensure '.' decimal separator in all locales
            val rlat = String.format(Locale.US, "%.2f", lat)
            val rlon = String.format(Locale.US, "%.2f", lon)
            val url = "$BASE_URL?latitude=$rlat&longitude=$rlon" +
                "&current=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m"
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                val code = connection.responseCode
                if (code !in 200..299) {
                    Log.e("WeatherService", "HTTP $code from Open-Meteo")
                    return@withContext null
                }
                val json = connection.inputStream.bufferedReader().use { reader ->
                    val sb = StringBuilder()
                    val buffer = CharArray(8_192)
                    var bytesRead: Int
                    var totalRead = 0
                    while (reader.read(buffer).also { bytesRead = it } > 0) {
                        totalRead += bytesRead
                        if (totalRead > 65_536) break // 64KB safety limit
                        sb.append(buffer, 0, bytesRead)
                    }
                    sb.toString()
                }
                parseWeatherJson(json)
            } catch (e: Exception) {
                Log.e("WeatherService", "Failed to fetch weather", e)
                null
            } finally {
                connection.disconnect()
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
            root.optJSONObject("current")?.let { current ->
                val temp = current.optDouble("temperature_2m", 0.0)
                val wind = current.optDouble("wind_speed_10m", 0.0)
                // Guard against NaN from malformed JSON values
                if (temp.isNaN() || wind.isNaN()) return@let null
                WeatherData(
                    temperatureCelsius = temp,
                    weatherCode = current.optInt("weather_code", -1),
                    windSpeedKmh = wind,
                    windDirectionDegrees = current.optInt("wind_direction_10m", 0)
                )
            }
        } catch (e: Exception) {
            Log.e("WeatherService", "Failed to parse weather JSON", e)
            null
        }
}
