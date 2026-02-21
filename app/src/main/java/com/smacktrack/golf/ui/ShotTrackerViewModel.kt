package com.smacktrack.golf.ui

/**
 * Central ViewModel for the shot tracking workflow.
 *
 * Manages the five-phase shot lifecycle (Club Select -> Calibrating Start ->
 * Walking -> Calibrating End -> Result) and holds all UI state including
 * shot history, live distance, and user settings.
 *
 * GPS positions are collected via [LocationProvider] and refined using
 * accuracy-weighted calibration before computing haversine distances.
 * Weather data is fetched from Open-Meteo API when a shot completes.
 */

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.domain.GpsCoordinate
import com.smacktrack.golf.location.GpsSample
import com.smacktrack.golf.location.LocationProvider
import com.smacktrack.golf.location.bearingDegrees
import com.smacktrack.golf.location.calibrateWeighted
import com.smacktrack.golf.location.haversineMeters
import com.smacktrack.golf.location.metersToYards
import com.smacktrack.golf.network.WeatherData
import com.smacktrack.golf.network.WeatherService
import com.smacktrack.golf.network.celsiusToFahrenheit
import com.smacktrack.golf.network.degreesToCompass
import com.smacktrack.golf.network.wmoCodeToLabel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ShotPhase {
    CLUB_SELECT,
    CALIBRATING_START,
    WALKING,
    CALIBRATING_END,
    RESULT
}

enum class DistanceUnit(val label: String) {
    YARDS("Yards"),
    METERS("Meters")
}

enum class WindUnit(val label: String) {
    KMH("km/h"),
    MPH("mph")
}

enum class TemperatureUnit(val label: String) {
    FAHRENHEIT("\u00B0F"),
    CELSIUS("\u00B0C")
}

enum class Trajectory(val label: String, val multiplier: Double) {
    LOW("Low", 0.75),
    MID("Mid", 1.0),
    HIGH("High", 1.30)
}

data class ShotResult(
    val club: Club,
    val distanceYards: Int,
    val distanceMeters: Int,
    val weatherDescription: String,
    val temperatureF: Int,
    val temperatureC: Int,
    val windSpeedKmh: Double,
    val windDirectionCompass: String,
    val windDirectionDegrees: Int = 0,
    val shotBearingDegrees: Double = 0.0
)

data class AppSettings(
    val distanceUnit: DistanceUnit = DistanceUnit.YARDS,
    val windUnit: WindUnit = WindUnit.MPH,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val trajectory: Trajectory = Trajectory.MID,
    val enabledClubs: Set<Club> = Club.entries.toSet()
)

data class ShotTrackerUiState(
    val phase: ShotPhase = ShotPhase.CLUB_SELECT,
    val selectedClub: Club? = Club.DRIVER,
    val startCoordinate: GpsCoordinate? = null,
    val liveDistanceYards: Int = 0,
    val liveDistanceMeters: Int = 0,
    val shotResult: ShotResult? = null,
    val shotHistory: List<ShotResult> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val locationPermissionGranted: Boolean = false
)

class ShotTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ShotTrackerUiState())
    val uiState: StateFlow<ShotTrackerUiState> = _uiState.asStateFlow()

    private val locationProvider = LocationProvider(application)

    // Current GPS position — updated by location flow from FusedLocationProviderClient.
    // Defaults are a fallback if GPS is unavailable.
    private var currentLat = 0.0
    private var currentLon = 0.0
    private var currentAccuracy = 100.0

    private var locationJob: Job? = null

    // Calibration: 2.5s window, 500ms intervals = 5 samples (6 with first discarded)
    private companion object {
        const val CALIBRATION_DURATION_MS = 2500L
        const val CALIBRATION_INTERVAL_MS = 500L
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationPermissionGranted = granted) }
    }

    private fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        if (!_uiState.value.locationPermissionGranted) return

        locationJob = viewModelScope.launch {
            locationProvider.locationUpdates(CALIBRATION_INTERVAL_MS).collect { update ->
                currentLat = update.lat
                currentLon = update.lon
                currentAccuracy = update.accuracyMeters.toDouble()
            }
        }
    }

    private fun stopLocationUpdates() {
        locationJob?.cancel()
        locationJob = null
    }

    fun selectClub(club: Club) {
        _uiState.update { it.copy(selectedClub = club) }
    }

    /**
     * Collects GPS samples over [CALIBRATION_DURATION_MS] at [CALIBRATION_INTERVAL_MS] intervals.
     * Returns the collected samples for use with [calibrateWeighted].
     */
    private suspend fun collectGpsSamples(): List<GpsSample> {
        val samples = mutableListOf<GpsSample>()
        val endTime = System.currentTimeMillis() + CALIBRATION_DURATION_MS

        while (System.currentTimeMillis() < endTime) {
            samples.add(
                GpsSample(
                    lat = currentLat,
                    lon = currentLon,
                    accuracyMeters = currentAccuracy
                )
            )
            delay(CALIBRATION_INTERVAL_MS)
        }

        return samples
    }

    fun markStart() {
        _uiState.value.selectedClub ?: return
        _uiState.update { it.copy(phase = ShotPhase.CALIBRATING_START) }

        startLocationUpdates()

        viewModelScope.launch {
            val samples = collectGpsSamples()
            val calibrated = calibrateWeighted(samples)

            val startCoord = calibrated?.coordinate
                ?: GpsCoordinate(currentLat, currentLon)

            _uiState.update {
                it.copy(
                    phase = ShotPhase.WALKING,
                    startCoordinate = startCoord,
                    liveDistanceYards = 0,
                    liveDistanceMeters = 0
                )
            }

            pollLiveDistance(startCoord)
        }
    }

    private suspend fun pollLiveDistance(startCoord: GpsCoordinate) {
        while (_uiState.value.phase == ShotPhase.WALKING) {
            delay(1000)

            val currentPos = GpsCoordinate(currentLat, currentLon)
            val distanceMeters = haversineMeters(startCoord, currentPos)
            val distanceYards = metersToYards(distanceMeters)

            _uiState.update {
                it.copy(
                    liveDistanceYards = distanceYards.roundToInt(),
                    liveDistanceMeters = distanceMeters.roundToInt()
                )
            }
        }
    }

    fun markEnd() {
        val state = _uiState.value
        val startCoord = state.startCoordinate ?: return
        val club = state.selectedClub ?: return

        _uiState.update { it.copy(phase = ShotPhase.CALIBRATING_END) }

        viewModelScope.launch {
            // Run GPS calibration and weather fetch in parallel
            val samplesDeferred = async { collectGpsSamples() }
            val weatherDeferred = async {
                if (currentLat != 0.0 || currentLon != 0.0) {
                    WeatherService.fetchWeather(currentLat, currentLon)
                } else null
            }

            val samples = samplesDeferred.await()
            val calibrated = calibrateWeighted(samples)

            val endCoord = calibrated?.coordinate
                ?: GpsCoordinate(currentLat, currentLon)

            val distanceMeters = haversineMeters(startCoord, endCoord)
            val distanceYards = metersToYards(distanceMeters)

            // Use real weather or fallback to "Unknown" defaults
            val weather = weatherDeferred.await() ?: WeatherData(
                temperatureCelsius = 0.0,
                weatherCode = -1,
                windSpeedKmh = 0.0,
                windDirectionDegrees = 0
            )

            stopLocationUpdates()

            val shotBearing = bearingDegrees(startCoord, endCoord)

            val result = ShotResult(
                club = club,
                distanceYards = distanceYards.roundToInt(),
                distanceMeters = distanceMeters.roundToInt(),
                weatherDescription = wmoCodeToLabel(weather.weatherCode),
                temperatureF = celsiusToFahrenheit(weather.temperatureCelsius).roundToInt(),
                temperatureC = weather.temperatureCelsius.roundToInt(),
                windSpeedKmh = weather.windSpeedKmh,
                windDirectionCompass = degreesToCompass(weather.windDirectionDegrees),
                windDirectionDegrees = weather.windDirectionDegrees,
                shotBearingDegrees = shotBearing
            )

            _uiState.update {
                it.copy(
                    phase = ShotPhase.RESULT,
                    shotResult = result,
                    shotHistory = it.shotHistory + result
                )
            }
        }
    }

    fun nextShot() {
        stopLocationUpdates()
        _uiState.update {
            it.copy(
                phase = ShotPhase.CLUB_SELECT,
                startCoordinate = null,
                liveDistanceYards = 0,
                liveDistanceMeters = 0,
                shotResult = null
            )
        }
    }

    fun reset() = nextShot()

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun updateDistanceUnit(unit: DistanceUnit) {
        _uiState.update { it.copy(settings = it.settings.copy(distanceUnit = unit)) }
    }

    fun updateWindUnit(unit: WindUnit) {
        _uiState.update { it.copy(settings = it.settings.copy(windUnit = unit)) }
    }

    fun updateTemperatureUnit(unit: TemperatureUnit) {
        _uiState.update { it.copy(settings = it.settings.copy(temperatureUnit = unit)) }
    }

    fun updateTrajectory(trajectory: Trajectory) {
        _uiState.update { it.copy(settings = it.settings.copy(trajectory = trajectory)) }
    }

    fun toggleClub(club: Club) {
        _uiState.update {
            val current = it.settings.enabledClubs
            val updated = if (club in current) current - club else current + club
            it.copy(settings = it.settings.copy(enabledClubs = updated))
        }
    }

    // ── Wind overrides ────────────────────────────────────────────────────────

    fun adjustWindDirection(deltaDegrees: Int) {
        _uiState.update { state ->
            val result = state.shotResult ?: return@update state
            val newDeg = (result.windDirectionDegrees + deltaDegrees + 360) % 360
            val updated = result.copy(
                windDirectionDegrees = newDeg,
                windDirectionCompass = degreesToCompass(newDeg)
            )
            val history = state.shotHistory.toMutableList()
            if (history.isNotEmpty()) history[history.lastIndex] = updated
            state.copy(shotResult = updated, shotHistory = history)
        }
    }

    fun adjustWindSpeed(deltaKmh: Double) {
        _uiState.update { state ->
            val result = state.shotResult ?: return@update state
            val updated = result.copy(
                windSpeedKmh = (result.windSpeedKmh + deltaKmh).coerceIn(0.0, 200.0)
            )
            val history = state.shotHistory.toMutableList()
            if (history.isNotEmpty()) history[history.lastIndex] = updated
            state.copy(shotResult = updated, shotHistory = history)
        }
    }
}
