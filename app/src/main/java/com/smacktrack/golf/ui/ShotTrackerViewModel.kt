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
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smacktrack.golf.data.AuthManager
import com.smacktrack.golf.data.ShotRepository
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
import kotlinx.coroutines.flow.collectLatest
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

enum class SyncStatus { IDLE, SYNCING, SYNCED, ERROR }

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
    val shotBearingDegrees: Double = 0.0,
    val timestampMs: Long = System.currentTimeMillis()
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
    val locationPermissionGranted: Boolean = false,
    val userEmail: String? = null,
    val isSignedIn: Boolean = false,
    val signInError: String? = null,
    val syncStatus: SyncStatus = SyncStatus.IDLE
)

class ShotTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShotRepository(application)
    var authManager: AuthManager? = null
        set(value) {
            field?.cleanup()
            field = value
            if (value != null) observeAuthState()
        }

    private fun toast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    private val _uiState = MutableStateFlow(ShotTrackerUiState())
    val uiState: StateFlow<ShotTrackerUiState> = _uiState.asStateFlow()

    private val locationProvider = LocationProvider(application)

    private var shotsCollectionJob: Job? = null
    private var settingsCollectionJob: Job? = null
    private var authObserverJob: Job? = null
    private var errorObserverJob: Job? = null

    init {
        val savedShots = repository.loadShots()
        val savedSettings = repository.loadSettings()
        _uiState.update { it.copy(shotHistory = savedShots, settings = savedSettings) }
    }

    private fun observeAuthState() {
        val auth = authManager ?: return
        authObserverJob?.cancel()
        errorObserverJob?.cancel()
        authObserverJob = viewModelScope.launch {
            auth.currentUser.collectLatest { user ->
                _uiState.update {
                    it.copy(
                        isSignedIn = user != null,
                        userEmail = user?.email
                    )
                }
                if (user != null) {
                    toast("Signed in as ${user.email}")
                    // Migrate local data on first sign-in
                    _uiState.update { it.copy(syncStatus = SyncStatus.SYNCING) }
                    try {
                        repository.migrateLocalToFirestore()
                        toast("Local data migrated to cloud")
                        _uiState.update { it.copy(syncStatus = SyncStatus.SYNCED) }
                    } catch (e: Exception) {
                        android.util.Log.e("ViewModel", "Migration failed", e)
                        toast("Migration failed: ${e.message}")
                        _uiState.update { it.copy(syncStatus = SyncStatus.ERROR) }
                    }
                } else {
                    toast("Signed out — using local storage")
                    _uiState.update { it.copy(syncStatus = SyncStatus.IDLE) }
                }
                // Re-subscribe to the correct data source
                subscribeToData()
            }
        }
        errorObserverJob = viewModelScope.launch {
            auth.signInError.collectLatest { error ->
                _uiState.update { it.copy(signInError = error) }
            }
        }
    }

    private fun subscribeToData() {
        shotsCollectionJob?.cancel()
        settingsCollectionJob?.cancel()

        shotsCollectionJob = viewModelScope.launch {
            repository.shotsFlow().collectLatest { shots ->
                _uiState.update { it.copy(shotHistory = shots) }
                repository.saveShots(shots)
            }
        }

        settingsCollectionJob = viewModelScope.launch {
            repository.settingsFlow().collectLatest { settings ->
                _uiState.update { it.copy(settings = settings) }
                repository.saveSettings(settings)
            }
        }
    }

    private fun firestoreSync(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(syncStatus = SyncStatus.SYNCING) }
            try {
                block()
                _uiState.update { it.copy(syncStatus = SyncStatus.SYNCED) }
            } catch (e: Exception) {
                android.util.Log.w("ViewModel", "Firestore write failed, retrying once", e)
                try {
                    delay(3000)
                    block()
                    _uiState.update { it.copy(syncStatus = SyncStatus.SYNCED) }
                } catch (e2: Exception) {
                    android.util.Log.e("ViewModel", "Firestore retry failed", e2)
                    _uiState.update { it.copy(syncStatus = SyncStatus.ERROR) }
                }
            }
        }
    }

    // Current GPS position — updated by location flow from FusedLocationProviderClient.
    // Wrapped in a data class so reads/writes are atomic (single reference swap).
    private data class GpsState(val lat: Double = 0.0, val lon: Double = 0.0, val accuracy: Double = 100.0)
    @Volatile private var gpsState = GpsState()

    private var locationJob: Job? = null

    // Calibration: start = 3.5s (7 samples), end = 2s (4 samples) at 500ms intervals
    // End calibration is shorter because GPS has been streaming during the walk.
    private companion object {
        const val CALIBRATION_DURATION_MS = 3500L
        const val END_CALIBRATION_DURATION_MS = 2000L
        const val CALIBRATION_INTERVAL_MS = 500L
        const val GPS_WARMUP_TIMEOUT_MS = 5000L
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationPermissionGranted = granted) }
    }

    private fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        if (!_uiState.value.locationPermissionGranted) return

        locationJob = viewModelScope.launch {
            locationProvider.locationUpdates(CALIBRATION_INTERVAL_MS).collect { update ->
                gpsState = GpsState(update.lat, update.lon, update.accuracyMeters.toDouble())
            }
        }
    }

    private fun stopLocationUpdates() {
        locationJob?.cancel()
        locationJob = null
    }

    private fun clearGpsState() {
        gpsState = GpsState()
    }

    /** Waits until the location flow delivers a fresh fix (non-zero lat/lon). */
    private suspend fun waitForFreshGps() {
        val deadline = System.currentTimeMillis() + GPS_WARMUP_TIMEOUT_MS
        while (gpsState.lat == 0.0 && gpsState.lon == 0.0 && System.currentTimeMillis() < deadline) {
            delay(CALIBRATION_INTERVAL_MS)
        }
    }

    fun selectClub(club: Club) {
        _uiState.update { it.copy(selectedClub = club) }
    }

    /**
     * Collects GPS samples over [durationMs] at [CALIBRATION_INTERVAL_MS] intervals.
     * Returns the collected samples for use with [calibrateWeighted].
     */
    private suspend fun collectGpsSamples(durationMs: Long = CALIBRATION_DURATION_MS): List<GpsSample> {
        val samples = mutableListOf<GpsSample>()
        val endTime = System.currentTimeMillis() + durationMs

        while (System.currentTimeMillis() < endTime) {
            val snap = gpsState
            samples.add(
                GpsSample(
                    lat = snap.lat,
                    lon = snap.lon,
                    accuracyMeters = snap.accuracy
                )
            )
            delay(CALIBRATION_INTERVAL_MS)
        }

        return samples
    }

    fun markStart() {
        // Default to Driver if no club selected yet
        if (_uiState.value.selectedClub == null) {
            _uiState.update { it.copy(selectedClub = Club.DRIVER) }
        }
        _uiState.update { it.copy(phase = ShotPhase.CALIBRATING_START) }

        startLocationUpdates()

        viewModelScope.launch {
            // Wait for fresh GPS fix before calibrating (stale coords were cleared on reset)
            waitForFreshGps()
            val samples = collectGpsSamples()
            val calibrated = calibrateWeighted(samples)

            val startCoord = calibrated?.coordinate
                ?: GpsCoordinate(gpsState.lat, gpsState.lon)

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

            val currentPos = GpsCoordinate(gpsState.lat, gpsState.lon)
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
        val capturedUid = repository.snapshotUid()

        _uiState.update { it.copy(phase = ShotPhase.CALIBRATING_END) }

        viewModelScope.launch {
            // Run GPS calibration and weather fetch in parallel
            val samplesDeferred = async { collectGpsSamples(END_CALIBRATION_DURATION_MS) }
            val weatherSnap = gpsState
            val weatherDeferred = async {
                if (weatherSnap.lat != 0.0 || weatherSnap.lon != 0.0) {
                    WeatherService.fetchWeather(weatherSnap.lat, weatherSnap.lon)
                } else null
            }

            val samples = samplesDeferred.await()
            val calibrated = calibrateWeighted(samples)

            val endCoord = calibrated?.coordinate
                ?: GpsCoordinate(gpsState.lat, gpsState.lon)

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

            // Update UI immediately — don't block on network
            _uiState.update {
                it.copy(
                    phase = ShotPhase.RESULT,
                    shotResult = result,
                    shotHistory = it.shotHistory + result
                )
            }
            // Persist to SharedPrefs as local backup
            persistShots()
            toast("Shot saved" + if (_uiState.value.isSignedIn) " to cloud" else " locally")
            // Save to Firestore in background (non-blocking, with retry)
            firestoreSync { repository.saveShot(result, forUid = capturedUid) }
        }
    }

    fun nextShot() {
        stopLocationUpdates()
        clearGpsState()
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
        authManager?.cleanup()
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun updateDistanceUnit(unit: DistanceUnit) {
        _uiState.update { it.copy(settings = it.settings.copy(distanceUnit = unit)) }
        persistSettings()
    }

    fun updateWindUnit(unit: WindUnit) {
        _uiState.update { it.copy(settings = it.settings.copy(windUnit = unit)) }
        persistSettings()
    }

    fun updateTemperatureUnit(unit: TemperatureUnit) {
        _uiState.update { it.copy(settings = it.settings.copy(temperatureUnit = unit)) }
        persistSettings()
    }

    fun updateTrajectory(trajectory: Trajectory) {
        _uiState.update { it.copy(settings = it.settings.copy(trajectory = trajectory)) }
        persistSettings()
    }

    fun toggleClub(club: Club) {
        _uiState.update {
            val current = it.settings.enabledClubs
            val updated = if (club in current) current - club else current + club
            it.copy(settings = it.settings.copy(enabledClubs = updated))
        }
        persistSettings()
    }

    // ── Wind overrides ────────────────────────────────────────────────────────

    fun deleteShot(index: Int) {
        val shot = _uiState.value.shotHistory.getOrNull(index) ?: return
        val capturedUid = repository.snapshotUid()
        _uiState.update {
            it.copy(shotHistory = it.shotHistory.filterIndexed { i, _ -> i != index })
        }
        persistShots()
        toast("Shot deleted" + if (_uiState.value.isSignedIn) " from cloud" else " locally")
        firestoreSync { repository.deleteShot(shot.timestampMs, forUid = capturedUid) }
    }

    fun adjustWindDirection(deltaDegrees: Int) {
        val capturedUid = repository.snapshotUid()
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
        persistShots()
        val result = _uiState.value.shotResult
        if (result != null) {
            firestoreSync { repository.updateShot(result, forUid = capturedUid) }
        }
    }

    fun adjustWindSpeed(deltaKmh: Double) {
        val capturedUid = repository.snapshotUid()
        _uiState.update { state ->
            val result = state.shotResult ?: return@update state
            val updated = result.copy(
                windSpeedKmh = (result.windSpeedKmh + deltaKmh).coerceIn(0.0, 200.0)
            )
            val history = state.shotHistory.toMutableList()
            if (history.isNotEmpty()) history[history.lastIndex] = updated
            state.copy(shotResult = updated, shotHistory = history)
        }
        persistShots()
        val result = _uiState.value.shotResult
        if (result != null) {
            firestoreSync { repository.updateShot(result, forUid = capturedUid) }
        }
    }

    // ── Auth actions (called from UI) ────────────────────────────────────────

    fun signInWithGoogle(webClientId: String) {
        val auth = authManager ?: return
        viewModelScope.launch {
            auth.signInWithGoogle(webClientId)
        }
    }

    fun clearSignInError() {
        authManager?.clearError()
        _uiState.update { it.copy(signInError = null) }
    }

    fun signOut() {
        val auth = authManager ?: return
        viewModelScope.launch {
            auth.signOut()
            // Reload local data after sign-out
            val localShots = repository.loadShots()
            val localSettings = repository.loadSettings()
            _uiState.update { it.copy(shotHistory = localShots, settings = localSettings) }
        }
    }

    // ── Persistence helpers ──────────────────────────────────────────────────

    private fun persistShots() {
        repository.saveShots(_uiState.value.shotHistory)
    }

    private fun persistSettings() {
        val capturedUid = repository.snapshotUid()
        val settings = _uiState.value.settings
        repository.saveSettings(settings)
        if (_uiState.value.isSignedIn) {
            firestoreSync { repository.saveSettingsToFirestore(settings, forUid = capturedUid) }
        }
    }
}
