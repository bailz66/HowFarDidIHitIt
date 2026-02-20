package com.smacktrack.golf.ui

/**
 * Central ViewModel for the shot tracking workflow.
 *
 * Manages the five-phase shot lifecycle (Club Select -> Calibrating Start ->
 * Walking -> Calibrating End -> Result) and holds all UI state including
 * shot history, live distance, and user settings.
 *
 * GPS positions are collected via [collectGpsSamples] and refined using
 * accuracy-weighted calibration before computing haversine distances.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.domain.GpsCoordinate
import com.smacktrack.golf.location.CalibratedPosition
import com.smacktrack.golf.location.GpsSample
import com.smacktrack.golf.location.bearingDegrees
import com.smacktrack.golf.location.calibrateWeighted
import com.smacktrack.golf.location.haversineMeters
import com.smacktrack.golf.location.metersToYards
import com.smacktrack.golf.location.yardsToMeters
import com.smacktrack.golf.network.WeatherData
import com.smacktrack.golf.network.celsiusToFahrenheit
import com.smacktrack.golf.network.degreesToCompass
import com.smacktrack.golf.network.wmoCodeToLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val settings: AppSettings = AppSettings()
)

class ShotTrackerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ShotTrackerUiState())
    val uiState: StateFlow<ShotTrackerUiState> = _uiState.asStateFlow()

    // Current GPS position — in production this comes from FusedLocationProviderClient.
    // On the emulator it stays fixed unless you change it in Extended Controls > Location.
    private var currentLat = 37.4220
    private var currentLon = -122.0841
    // Simulated GPS accuracy in meters — on a real device this comes from Location.accuracy
    private var currentAccuracy = 5.0

    // Calibration: 2.5s window, 500ms intervals = 5 samples (6 with first discarded)
    private companion object {
        const val CALIBRATION_DURATION_MS = 2500L
        const val CALIBRATION_INTERVAL_MS = 500L
    }

    fun selectClub(club: Club) {
        _uiState.value = _uiState.value.copy(selectedClub = club)
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
        _uiState.value = _uiState.value.copy(phase = ShotPhase.CALIBRATING_START)

        viewModelScope.launch {
            val samples = collectGpsSamples()
            val calibrated = calibrateWeighted(samples)

            val startCoord = calibrated?.coordinate
                ?: GpsCoordinate(currentLat, currentLon)

            _uiState.value = _uiState.value.copy(
                phase = ShotPhase.WALKING,
                startCoordinate = startCoord,
                liveDistanceYards = 0,
                liveDistanceMeters = 0
            )

            pollLiveDistance(startCoord)
        }
    }

    private suspend fun pollLiveDistance(startCoord: GpsCoordinate) {
        while (_uiState.value.phase == ShotPhase.WALKING) {
            delay(1000)

            val currentPos = GpsCoordinate(currentLat, currentLon)
            val distanceMeters = haversineMeters(startCoord, currentPos)
            val distanceYards = metersToYards(distanceMeters)

            _uiState.value = _uiState.value.copy(
                liveDistanceYards = distanceYards.roundToInt(),
                liveDistanceMeters = distanceMeters.roundToInt()
            )
        }
    }

    fun markEnd() {
        val state = _uiState.value
        val startCoord = state.startCoordinate ?: return
        val club = state.selectedClub ?: return

        _uiState.value = state.copy(phase = ShotPhase.CALIBRATING_END)

        viewModelScope.launch {
            val samples = collectGpsSamples()
            val calibrated = calibrateWeighted(samples)

            val endCoord = calibrated?.coordinate
                ?: GpsCoordinate(currentLat, currentLon)

            val distanceMeters = haversineMeters(startCoord, endCoord)
            val distanceYards = metersToYards(distanceMeters)

            // Simulated weather data — in production this comes from weather API
            val weather = WeatherData(
                temperatureCelsius = 22.0,
                weatherCode = 1,
                windSpeedKmh = 12.0,
                windDirectionDegrees = 315
            )

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

            _uiState.value = _uiState.value.copy(
                phase = ShotPhase.RESULT,
                shotResult = result,
                shotHistory = _uiState.value.shotHistory + result
            )
        }
    }

    fun nextShot() {
        _uiState.value = _uiState.value.copy(
            phase = ShotPhase.CLUB_SELECT,
            startCoordinate = null,
            liveDistanceYards = 0,
            liveDistanceMeters = 0,
            shotResult = null
        )
    }

    fun reset() {
        _uiState.value = _uiState.value.copy(
            phase = ShotPhase.CLUB_SELECT,
            startCoordinate = null,
            liveDistanceYards = 0,
            liveDistanceMeters = 0,
            shotResult = null
        )
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun updateDistanceUnit(unit: DistanceUnit) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(distanceUnit = unit)
        )
    }

    fun updateWindUnit(unit: WindUnit) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(windUnit = unit)
        )
    }

    fun updateTemperatureUnit(unit: TemperatureUnit) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(temperatureUnit = unit)
        )
    }

    fun updateTrajectory(trajectory: Trajectory) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(trajectory = trajectory)
        )
    }

    fun toggleClub(club: Club) {
        val current = _uiState.value.settings.enabledClubs
        val updated = if (club in current) current - club else current + club
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(enabledClubs = updated)
        )
    }
}
