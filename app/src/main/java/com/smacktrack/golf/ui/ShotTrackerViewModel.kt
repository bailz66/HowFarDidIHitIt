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
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.smacktrack.golf.data.AchievementRepository
import com.smacktrack.golf.service.ShotTrackingService
import com.smacktrack.golf.data.AnalyticsTracker
import com.smacktrack.golf.data.AuthManager
import com.smacktrack.golf.data.ShotRepository
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.domain.UnlockedAchievement
import com.smacktrack.golf.domain.checkAchievements
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val unlockedAchievements: Map<String, Long> = emptyMap(),
    val newlyUnlockedAchievements: List<UnlockedAchievement> = emptyList(),
    val gpsAccuracyMeters: Double? = null,
    val accountDeleted: Boolean = false
)

class ShotTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShotRepository(application)
    private val achievementRepository = AchievementRepository(application)
    private val analyticsTracker = AnalyticsTracker(FirebaseAnalytics.getInstance(application))
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
    private var achievementsCollectionJob: Job? = null
    private var authObserverJob: Job? = null
    private var errorObserverJob: Job? = null
    private var startAccuracyMeters: Double? = null

    init {
        achievementRepository.migrateOldKeys()
        val savedShots = repository.loadShots()
        val savedSettings = repository.loadSettings()
        val savedAchievements = achievementRepository.loadUnlocked()
        _uiState.update {
            it.copy(
                shotHistory = savedShots,
                settings = savedSettings,
                unlockedAchievements = savedAchievements
            )
        }
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
                    // Migrate local data on first sign-in.
                    // Use NonCancellable so a rapid auth re-emission can't leave
                    // migration half-done (data uploaded but local not yet cleared).
                    _uiState.update { it.copy(syncStatus = SyncStatus.SYNCING) }
                    try {
                        withContext(NonCancellable) {
                            repository.migrateLocalToFirestore()
                            achievementRepository.migrateLocalToFirestore()
                        }
                        toast("Local data migrated to cloud")
                        _uiState.update { it.copy(syncStatus = SyncStatus.SYNCED) }
                    } catch (e: Exception) {
                        Log.e("ShotTrackerVM", "Migration failed", e)
                        toast("Migration failed. Please try again.")
                        _uiState.update { it.copy(syncStatus = SyncStatus.ERROR) }
                    }
                } else {
                    toast("Signed out. Using local storage.")
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
        achievementsCollectionJob?.cancel()

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

        achievementsCollectionJob = viewModelScope.launch {
            achievementRepository.achievementsFlow().collectLatest { achievements ->
                _uiState.update { it.copy(unlockedAchievements = achievements) }
                achievementRepository.saveUnlocked(achievements)
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
                Log.e("ViewModel", "Firestore write failed", e)
                _uiState.update { it.copy(syncStatus = SyncStatus.ERROR) }
            }
        }
    }

    // Current GPS position — updated by location flow from FusedLocationProviderClient.
    // Wrapped in a data class so reads/writes are atomic (single reference swap).
    private data class GpsState(val lat: Double = 0.0, val lon: Double = 0.0, val accuracy: Double = 100.0)
    @Volatile private var gpsState = GpsState()

    private var locationJob: Job? = null
    private var shotTimeoutJob: Job? = null

    // Calibration: start = 3.5s (7 samples), end = 2s (4 samples) at 500ms intervals
    // End calibration is shorter because GPS has been streaming during the walk.
    private companion object {
        const val CALIBRATION_DURATION_MS = 3500L
        const val END_CALIBRATION_DURATION_MS = 2000L
        const val CALIBRATION_INTERVAL_MS = 500L
        const val GPS_WARMUP_TIMEOUT_MS = 5000L
        const val SHOT_TIMEOUT_MS = 15 * 60 * 1000L
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

    private fun startTrackingService() {
        ShotTrackingService.start(getApplication())
    }

    private fun stopTrackingService() {
        ShotTrackingService.stop(getApplication())
    }

    private fun startShotTimeout() {
        shotTimeoutJob?.cancel()
        shotTimeoutJob = viewModelScope.launch {
            delay(SHOT_TIMEOUT_MS)
            toast("Shot timed out after 15 minutes")
            nextShot()
        }
    }

    private fun cancelShotTimeout() {
        shotTimeoutJob?.cancel()
        shotTimeoutJob = null
    }

    fun markStart() {
        if (_uiState.value.phase != ShotPhase.CLUB_SELECT) return
        // Default to Driver if no club selected yet
        if (_uiState.value.selectedClub == null) {
            _uiState.update { it.copy(selectedClub = Club.DRIVER) }
        }
        _uiState.update { it.copy(phase = ShotPhase.CALIBRATING_START) }

        startLocationUpdates()
        if (_uiState.value.locationPermissionGranted) {
            startTrackingService()
        }
        startShotTimeout()

        viewModelScope.launch {
            // Wait for fresh GPS fix before calibrating (stale coords were cleared on reset)
            waitForFreshGps()
            val samples = collectGpsSamples()
            val calibrated = calibrateWeighted(samples)

            val snap = gpsState
            val startCoord = calibrated?.coordinate
                ?: if (snap.lat != 0.0 || snap.lon != 0.0) {
                    GpsCoordinate(snap.lat, snap.lon)
                } else {
                    // No valid GPS position — abort and return to club select
                    toast("Could not get GPS position. Try again in an open area.")
                    _uiState.update { it.copy(phase = ShotPhase.CLUB_SELECT) }
                    stopLocationUpdates()
                    stopTrackingService()
                    cancelShotTimeout()
                    return@launch
                }

            startAccuracyMeters = calibrated?.estimatedAccuracyMeters

            // Re-check phase — user may have tapped Reset during calibration
            if (_uiState.value.phase != ShotPhase.CALIBRATING_START) return@launch

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
            delay(500)

            val snap = gpsState
            if (snap.lat == 0.0 && snap.lon == 0.0) continue

            val currentPos = GpsCoordinate(snap.lat, snap.lon)
            val distanceMeters = haversineMeters(startCoord, currentPos)
            val distanceYards = metersToYards(distanceMeters)

            if (!distanceYards.isNaN() && !distanceMeters.isNaN()) {
                _uiState.update {
                    it.copy(
                        liveDistanceYards = distanceYards.roundToInt(),
                        liveDistanceMeters = distanceMeters.roundToInt()
                    )
                }
            }
        }
    }

    fun markEnd() {
        val state = _uiState.value
        if (state.phase != ShotPhase.WALKING) return
        val startCoord = state.startCoordinate ?: return
        val club = state.selectedClub ?: return
        val capturedUid = repository.snapshotUid()

        // Cancel timeout immediately — user tapped TRACK, shot isn't abandoned
        cancelShotTimeout()
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

            // If user tapped Reset during calibration, abort — don't create a ghost shot
            if (_uiState.value.phase != ShotPhase.CALIBRATING_END) return@launch

            val calibrated = calibrateWeighted(samples)

            val endSnap = gpsState
            val endCoord = calibrated?.coordinate
                ?: if (endSnap.lat != 0.0 || endSnap.lon != 0.0) {
                    GpsCoordinate(endSnap.lat, endSnap.lon)
                } else {
                    // No valid GPS — fall back to start coord (0 distance shot)
                    toast("GPS signal lost. Distance may be inaccurate.")
                    startCoord
                }

            var distanceMeters = haversineMeters(startCoord, endCoord)
            var distanceYards = metersToYards(distanceMeters)

            // Clamp implausible distances (NaN, infinite, or >500 yards)
            if (distanceYards.isNaN() || distanceYards.isInfinite()) {
                toast("GPS reading error. Distance could not be calculated.")
                distanceYards = 0.0
                distanceMeters = 0.0
            } else if (distanceYards > 500) {
                toast("Distance exceeded 500 yards. Capped for accuracy.")
                distanceYards = 500.0
                distanceMeters = 500.0 / 1.09361
            }

            // Use real weather or fallback — 21.1°C (70°F) baseline so temperature effect is zero
            val weather = weatherDeferred.await() ?: WeatherData(
                temperatureCelsius = 21.1,
                weatherCode = -1,
                windSpeedKmh = 0.0,
                windDirectionDegrees = 0
            )

            stopLocationUpdates()
            stopTrackingService()

            // Combine start + end accuracy (worst of the two)
            val endAccuracy = calibrated?.estimatedAccuracyMeters
            val combinedAccuracy = listOfNotNull(startAccuracyMeters, endAccuracy).maxOrNull()

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

            // Log analytics event first (no PII/location data)
            analyticsTracker.logShot(result)

            // Update UI immediately — don't block on network
            var updatedHistory: List<ShotResult> = emptyList()
            _uiState.update {
                val history = it.shotHistory + result
                updatedHistory = history
                it.copy(
                    phase = ShotPhase.RESULT,
                    shotResult = result,
                    shotHistory = history,
                    gpsAccuracyMeters = combinedAccuracy
                )
            }
            // Persist to SharedPrefs as local backup
            persistShots(updatedHistory)
            toast("Shot saved" + if (_uiState.value.isSignedIn) " to cloud" else " locally")
            // Save to Firestore in background (only when signed in to avoid duplicate local save)
            if (capturedUid != null) {
                firestoreSync { repository.saveShot(result, forUid = capturedUid) }
                viewModelScope.launch { repository.incrementGlobalShotCount() }
            }

            // Check achievements
            val currentState = _uiState.value
            val newAchievements = checkAchievements(
                allShots = currentState.shotHistory,
                newShot = result,
                alreadyUnlocked = currentState.unlockedAchievements.keys,
                enabledClubs = currentState.settings.enabledClubs
            )
            if (newAchievements.isNotEmpty()) {
                val now = System.currentTimeMillis()
                var mergedMap: Map<String, Long> = emptyMap()
                _uiState.update { state ->
                    val merged = state.unlockedAchievements.toMutableMap()
                    newAchievements.forEach { a -> merged[a.storageKey] = now }
                    mergedMap = merged
                    state.copy(
                        unlockedAchievements = merged,
                        newlyUnlockedAchievements = newAchievements
                    )
                }
                achievementRepository.saveUnlocked(mergedMap)
                newAchievements.forEach { achievement ->
                    analyticsTracker.logAchievement(achievement.category.name, achievement.tier.name)
                    if (capturedUid != null) {
                        firestoreSync {
                            achievementRepository.saveToFirestore(achievement.storageKey, now, capturedUid)
                        }
                    }
                }
            }
        }
    }

    fun changeResultClub(club: Club) {
        val capturedUid = repository.snapshotUid()
        var updatedResult: ShotResult? = null
        var updatedHistory: List<ShotResult> = emptyList()
        _uiState.update { state ->
            val result = state.shotResult ?: return@update state
            val updated = result.copy(club = club)
            val history = state.shotHistory.toMutableList()
            val idx = history.indexOfFirst { it.timestampMs == result.timestampMs }
            if (idx >= 0) history[idx] = updated
            updatedResult = updated
            updatedHistory = history
            state.copy(shotResult = updated, shotHistory = history)
        }
        persistShots(updatedHistory)
        if (capturedUid != null) {
            updatedResult?.let { result ->
                firestoreSync { repository.updateShot(result, forUid = capturedUid) }
            }
        }
        // Re-check achievements with the updated club
        updatedResult?.let { shot ->
            val currentState = _uiState.value
            val newAchievements = checkAchievements(
                allShots = currentState.shotHistory,
                newShot = shot,
                alreadyUnlocked = currentState.unlockedAchievements.keys,
                enabledClubs = currentState.settings.enabledClubs
            )
            if (newAchievements.isNotEmpty()) {
                val now = System.currentTimeMillis()
                var mergedMap: Map<String, Long> = emptyMap()
                _uiState.update { state ->
                    val merged = state.unlockedAchievements.toMutableMap()
                    newAchievements.forEach { a -> merged[a.storageKey] = now }
                    mergedMap = merged
                    state.copy(
                        unlockedAchievements = merged,
                        newlyUnlockedAchievements = newAchievements
                    )
                }
                achievementRepository.saveUnlocked(mergedMap)
                newAchievements.forEach { achievement ->
                    analyticsTracker.logAchievement(achievement.category.name, achievement.tier.name)
                    if (capturedUid != null) {
                        firestoreSync {
                            achievementRepository.saveToFirestore(achievement.storageKey, now, capturedUid)
                        }
                    }
                }
            }
        }
    }

    fun clearNewAchievements() {
        _uiState.update { it.copy(newlyUnlockedAchievements = emptyList()) }
    }

    fun nextShot() {
        stopLocationUpdates()
        stopTrackingService()
        cancelShotTimeout()
        clearGpsState()
        startAccuracyMeters = null
        _uiState.update {
            it.copy(
                phase = ShotPhase.CLUB_SELECT,
                startCoordinate = null,
                liveDistanceYards = 0,
                liveDistanceMeters = 0,
                shotResult = null,
                gpsAccuracyMeters = null
            )
        }
    }

    fun reset() = nextShot()

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        stopTrackingService()
        cancelShotTimeout()
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
            // Prevent disabling the last club
            if (club in current && current.size <= 1) return@update it
            val updated = if (club in current) current - club else current + club
            it.copy(settings = it.settings.copy(enabledClubs = updated))
        }
        persistSettings()
    }

    // ── Shot management ────────────────────────────────────────────────────────

    fun deleteShot(timestampMs: Long) {
        val capturedUid = repository.snapshotUid()
        var updatedHistory: List<ShotResult> = emptyList()
        _uiState.update {
            val filtered = it.shotHistory.filter { s -> s.timestampMs != timestampMs }
            updatedHistory = filtered
            it.copy(shotHistory = filtered)
        }
        persistShots(updatedHistory)
        toast("Shot deleted" + if (_uiState.value.isSignedIn) " from cloud" else " locally")
        if (capturedUid != null) {
            firestoreSync { repository.deleteShot(timestampMs, forUid = capturedUid) }
        }
    }

    // ── Wind overrides ────────────────────────────────────────────────────────

    fun adjustWindDirection(deltaDegrees: Int) {
        val capturedUid = repository.snapshotUid()
        var updatedResult: ShotResult? = null
        var updatedHistory: List<ShotResult> = emptyList()
        _uiState.update { state ->
            val result = state.shotResult ?: return@update state
            val newDeg = (result.windDirectionDegrees + deltaDegrees + 360) % 360
            val updated = result.copy(
                windDirectionDegrees = newDeg,
                windDirectionCompass = degreesToCompass(newDeg)
            )
            val history = state.shotHistory.toMutableList()
            val idx = history.indexOfFirst { it.timestampMs == result.timestampMs }
            if (idx >= 0) history[idx] = updated
            updatedResult = updated
            updatedHistory = history
            state.copy(shotResult = updated, shotHistory = history)
        }
        persistShots(updatedHistory)
        if (capturedUid != null) {
            updatedResult?.let { result ->
                firestoreSync { repository.updateShot(result, forUid = capturedUid) }
            }
        }
    }

    fun adjustWindSpeed(deltaKmh: Double) {
        val capturedUid = repository.snapshotUid()
        var updatedResult: ShotResult? = null
        var updatedHistory: List<ShotResult> = emptyList()
        _uiState.update { state ->
            val result = state.shotResult ?: return@update state
            val updated = result.copy(
                windSpeedKmh = (result.windSpeedKmh + deltaKmh).coerceIn(0.0, 100.0)
            )
            val history = state.shotHistory.toMutableList()
            val idx = history.indexOfFirst { it.timestampMs == result.timestampMs }
            if (idx >= 0) history[idx] = updated
            updatedResult = updated
            updatedHistory = history
            state.copy(shotResult = updated, shotHistory = history)
        }
        persistShots(updatedHistory)
        if (capturedUid != null) {
            updatedResult?.let { result ->
                firestoreSync { repository.updateShot(result, forUid = capturedUid) }
            }
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
        // Snapshot current Firestore-backed data to local storage BEFORE
        // cancelling listeners, so sign-out doesn't lose shot history
        // (migrateLocalToFirestore clears local prefs after upload).
        val currentState = _uiState.value
        repository.saveShots(currentState.shotHistory)
        repository.saveSettings(currentState.settings)
        achievementRepository.saveUnlocked(currentState.unlockedAchievements)
        // Cancel Firestore listeners BEFORE signing out to prevent stale data
        // from being written to SharedPrefs during the race window
        shotsCollectionJob?.cancel()
        settingsCollectionJob?.cancel()
        achievementsCollectionJob?.cancel()
        viewModelScope.launch {
            auth.signOut()
            // Reload local data after sign-out (now populated from snapshot above)
            val localShots = repository.loadShots()
            val localSettings = repository.loadSettings()
            val localAchievements = achievementRepository.loadUnlocked()
            _uiState.update {
                it.copy(
                    shotHistory = localShots,
                    settings = localSettings,
                    unlockedAchievements = localAchievements
                )
            }
        }
    }

    fun deleteAccount() {
        val auth = authManager ?: return
        val capturedUid = repository.snapshotUid() ?: return
        // Snapshot data to local storage before deletion
        val currentState = _uiState.value
        repository.saveShots(currentState.shotHistory)
        repository.saveSettings(currentState.settings)
        achievementRepository.saveUnlocked(currentState.unlockedAchievements)
        // Cancel Firestore listeners
        shotsCollectionJob?.cancel()
        settingsCollectionJob?.cancel()
        achievementsCollectionJob?.cancel()
        viewModelScope.launch {
            try {
                // Delete Firestore data first (needs auth token)
                repository.deleteAllUserData(capturedUid)
                // Then delete the Firebase Auth account
                auth.deleteAccount()
                // Reload local data
                val localShots = repository.loadShots()
                val localSettings = repository.loadSettings()
                val localAchievements = achievementRepository.loadUnlocked()
                _uiState.update {
                    it.copy(
                        shotHistory = localShots,
                        settings = localSettings,
                        unlockedAchievements = localAchievements,
                        accountDeleted = true
                    )
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Account deletion failed", e)
                _uiState.update { it.copy(signInError = "Account deletion failed. Please try again.") }
            }
        }
    }

    fun clearAccountDeletedFlag() {
        _uiState.update { it.copy(accountDeleted = false) }
    }

    // ── Persistence helpers ──────────────────────────────────────────────────

    private fun persistShots(shots: List<ShotResult> = _uiState.value.shotHistory) {
        repository.saveShots(shots)
    }

    private fun persistSettings() {
        val capturedUid = repository.snapshotUid()
        val settings = _uiState.value.settings
        repository.saveSettings(settings)
        if (capturedUid != null) {
            firestoreSync { repository.saveSettingsToFirestore(settings, forUid = capturedUid) }
        }
    }
}
