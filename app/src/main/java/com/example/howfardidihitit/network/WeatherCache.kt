package com.example.howfardidihitit.network

private const val CACHE_DURATION_MS = 60 * 60 * 1000L // 1 hour

data class WeatherData(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int
)

/**
 * Simple in-memory weather cache with 1-hour expiry.
 *
 * @param clock Function returning current time in milliseconds. Defaults to [System.currentTimeMillis].
 */
class WeatherCache(private val clock: () -> Long = System::currentTimeMillis) {

    private var cachedData: WeatherData? = null
    private var cacheTimestamp: Long = 0L

    fun get(): WeatherData? {
        val data = cachedData ?: return null
        val age = clock() - cacheTimestamp
        return if (age < CACHE_DURATION_MS) data else null
    }

    fun put(data: WeatherData) {
        cachedData = data
        cacheTimestamp = clock()
    }

    fun clear() {
        cachedData = null
        cacheTimestamp = 0L
    }
}
