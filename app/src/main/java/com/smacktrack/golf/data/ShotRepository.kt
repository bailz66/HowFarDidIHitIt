package com.smacktrack.golf.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smacktrack.golf.domain.Club
import com.smacktrack.golf.ui.AppSettings
import com.smacktrack.golf.ui.DistanceUnit
import com.smacktrack.golf.ui.ShotResult
import com.smacktrack.golf.ui.TemperatureUnit
import com.smacktrack.golf.ui.Trajectory
import com.smacktrack.golf.ui.WindUnit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

class ShotRepository(context: Context) {

    private val prefs = context.getSharedPreferences("smacktrack_data", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()

    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    fun snapshotUid(): String? = uid

    // ── Shots ────────────────────────────────────────────────────────────────

    fun shotsFlow(): Flow<List<ShotResult>> {
        val currentUid = uid ?: return flowOf(loadShotsFromPrefs())
        return callbackFlow {
            val registration = firestore.collection("users").document(currentUid)
                .collection("shots")
                .orderBy("timestampMs", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ShotRepository", "Firestore shots listener error", error)
                        trySend(loadShotsFromPrefs())
                        return@addSnapshotListener
                    }
                    val shots = snapshot?.documents?.mapNotNull { doc ->
                        try { doc.toShotResult() } catch (e: Exception) {
                            Log.e("ShotRepository", "Failed to parse shot doc", e)
                            null
                        }
                    }?.distinctBy { it.timestampMs }
                        ?.sortedBy { it.timestampMs }
                        ?: emptyList()
                    trySend(shots)
                }
            awaitClose { registration.remove() }
        }
    }

    fun loadShots(): List<ShotResult> {
        return loadShotsFromPrefs()
    }

    suspend fun saveShot(shot: ShotResult, forUid: String? = null) {
        val currentUid = forUid ?: uid
        if (currentUid != null) {
            firestore.collection("users").document(currentUid)
                .collection("shots")
                .document(shot.timestampMs.toString())
                .set(shot.toFirestoreMap())
                .await()
        } else {
            val shots = loadShotsFromPrefs() + shot
            saveShotsToPrefs(shots)
        }
    }

    suspend fun deleteShot(timestampMs: Long, forUid: String? = null) {
        val currentUid = forUid ?: uid
        if (currentUid != null) {
            val query = firestore.collection("users").document(currentUid)
                .collection("shots")
                .whereEqualTo("timestampMs", timestampMs)
                .get()
                .await()
            for (doc in query.documents) {
                doc.reference.delete().await()
            }
        } else {
            val shots = loadShotsFromPrefs().filter { it.timestampMs != timestampMs }
            saveShotsToPrefs(shots)
        }
    }

    fun saveShots(shots: List<ShotResult>) {
        saveShotsToPrefs(shots)
    }

    suspend fun updateShot(shot: ShotResult, forUid: String? = null) {
        val currentUid = forUid ?: uid
        if (currentUid != null) {
            val query = firestore.collection("users").document(currentUid)
                .collection("shots")
                .whereEqualTo("timestampMs", shot.timestampMs)
                .get()
                .await()
            for (doc in query.documents) {
                doc.reference.set(shot.toFirestoreMap()).await()
            }
        }
        // SharedPrefs path: caller manages the full list via saveShots()
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    fun settingsFlow(): Flow<AppSettings> {
        val currentUid = uid ?: return flowOf(loadSettingsFromPrefs())
        return callbackFlow {
            val registration = firestore.collection("users").document(currentUid)
                .collection("settings").document("prefs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ShotRepository", "Firestore settings listener error", error)
                        trySend(loadSettingsFromPrefs())
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(snapshot.toAppSettings())
                    } else {
                        trySend(AppSettings())
                    }
                }
            awaitClose { registration.remove() }
        }
    }

    fun loadSettings(): AppSettings {
        return loadSettingsFromPrefs()
    }

    suspend fun saveSettingsToFirestore(settings: AppSettings, forUid: String? = null) {
        val currentUid = forUid ?: uid ?: return
        firestore.collection("users").document(currentUid)
            .collection("settings").document("prefs")
            .set(settings.toFirestoreMap())
            .await()
    }

    fun saveSettings(settings: AppSettings) {
        saveSettingsToPrefs(settings)
    }

    // ── Migration ────────────────────────────────────────────────────────────

    suspend fun migrateLocalToFirestore() {
        val currentUid = uid ?: return

        val localShots = loadShotsFromPrefs()
        if (localShots.isNotEmpty()) {
            val shotsRef = firestore.collection("users").document(currentUid)
                .collection("shots")
            for (chunk in localShots.chunked(500)) {
                val batch = firestore.batch()
                chunk.forEach { shot ->
                    batch.set(shotsRef.document(shot.timestampMs.toString()), shot.toFirestoreMap())
                }
                batch.commit().await()
            }
            // Clear local shots after successful upload to prevent divergent data
            prefs.edit().remove("shot_history").apply()
        }

        // Settings are always synced (idempotent)
        val localSettings = loadSettingsFromPrefs()
        firestore.collection("users").document(currentUid)
            .collection("settings").document("prefs")
            .set(localSettings.toFirestoreMap())
            .await()
    }

    // ── SharedPreferences (local) ────────────────────────────────────────────

    private fun loadShotsFromPrefs(): List<ShotResult> {
        val json = prefs.getString("shot_history", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i -> array.getJSONObject(i).toShotResult() }
        } catch (e: Exception) {
            Log.e("ShotRepository", "Failed to load shots", e)
            emptyList()
        }
    }

    private fun saveShotsToPrefs(shots: List<ShotResult>) {
        val array = JSONArray()
        shots.forEach { array.put(it.toJson()) }
        prefs.edit().putString("shot_history", array.toString()).apply()
    }

    private fun loadSettingsFromPrefs(): AppSettings {
        return try {
            AppSettings(
                distanceUnit = prefs.getString("distance_unit", null)
                    ?.let { enumValueOfOrNull<DistanceUnit>(it) }
                    ?: DistanceUnit.YARDS,
                windUnit = prefs.getString("wind_unit", null)
                    ?.let { enumValueOfOrNull<WindUnit>(it) }
                    ?: WindUnit.MPH,
                temperatureUnit = prefs.getString("temperature_unit", null)
                    ?.let { enumValueOfOrNull<TemperatureUnit>(it) }
                    ?: TemperatureUnit.FAHRENHEIT,
                trajectory = prefs.getString("trajectory", null)
                    ?.let { enumValueOfOrNull<Trajectory>(it) }
                    ?: Trajectory.MID,
                enabledClubs = loadEnabledClubs()
            )
        } catch (e: Exception) {
            Log.e("ShotRepository", "Failed to load settings", e)
            AppSettings()
        }
    }

    private fun saveSettingsToPrefs(settings: AppSettings) {
        prefs.edit()
            .putString("distance_unit", settings.distanceUnit.name)
            .putString("wind_unit", settings.windUnit.name)
            .putString("temperature_unit", settings.temperatureUnit.name)
            .putString("trajectory", settings.trajectory.name)
            .putString("enabled_clubs", JSONArray().apply {
                settings.enabledClubs.forEach { put(it.name) }
            }.toString())
            .apply()
    }

    private fun loadEnabledClubs(): Set<Club> {
        val json = prefs.getString("enabled_clubs", null) ?: return Club.entries.toSet()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                enumValueOfOrNull<Club>(array.getString(i))
            }.toSet()
        } catch (e: Exception) {
            Log.e("ShotRepository", "Failed to load enabled clubs", e)
            Club.entries.toSet()
        }
    }

    // ── Firestore document deserialization (keeps Firestore SDK types private) ──

    private fun com.google.firebase.firestore.DocumentSnapshot.toShotResult(): ShotResult {
        return ShotResult(
            club = enumValueOfOrNull<Club>(getString("club") ?: "") ?: Club.DRIVER,
            distanceYards = getLong("distanceYards")?.toInt() ?: 0,
            distanceMeters = getLong("distanceMeters")?.toInt() ?: 0,
            weatherDescription = getString("weatherDescription") ?: "",
            temperatureF = getLong("temperatureF")?.toInt() ?: 0,
            temperatureC = getLong("temperatureC")?.toInt() ?: 0,
            windSpeedKmh = getDouble("windSpeedKmh") ?: 0.0,
            windDirectionCompass = getString("windDirectionCompass") ?: "",
            windDirectionDegrees = getLong("windDirectionDegrees")?.toInt() ?: 0,
            shotBearingDegrees = getDouble("shotBearingDegrees") ?: 0.0,
            timestampMs = getLong("timestampMs") ?: System.currentTimeMillis()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toAppSettings(): AppSettings {
        return AppSettings(
            distanceUnit = getString("distanceUnit")
                ?.let { enumValueOfOrNull<DistanceUnit>(it) } ?: DistanceUnit.YARDS,
            windUnit = getString("windUnit")
                ?.let { enumValueOfOrNull<WindUnit>(it) } ?: WindUnit.MPH,
            temperatureUnit = getString("temperatureUnit")
                ?.let { enumValueOfOrNull<TemperatureUnit>(it) } ?: TemperatureUnit.FAHRENHEIT,
            trajectory = getString("trajectory")
                ?.let { enumValueOfOrNull<Trajectory>(it) } ?: Trajectory.MID,
            enabledClubs = (get("enabledClubs") as? List<String>)
                ?.mapNotNull { enumValueOfOrNull<Club>(it) }?.toSet()
                ?: Club.entries.toSet()
        )
    }
}
