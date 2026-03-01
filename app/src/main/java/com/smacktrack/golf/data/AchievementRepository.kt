package com.smacktrack.golf.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class AchievementRepository(context: Context) {

    private val prefs = context.getSharedPreferences("smacktrack_achievements", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()

    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    // ── Migration from old flat keys to tiered keys ─────────────────────────

    private val oldToNewMapping = mapOf(
        "FIRST_BLOOD" to listOf("SHOT_COUNT_BRONZE"),
        "CENTURY" to listOf("SHOT_COUNT_BRONZE", "SHOT_COUNT_SILVER", "SHOT_COUNT_GOLD"),
        "CLUB_250" to listOf("BOMBER_BRONZE", "BOMBER_SILVER", "BOMBER_GOLD"),
        "FULL_BAG" to listOf(
            "FULL_BAG_BRONZE", "FULL_BAG_SILVER", "FULL_BAG_GOLD",
            "FULL_BAG_PLATINUM", "FULL_BAG_DIAMOND"
        ),
        "PB_MACHINE" to listOf("PB_MACHINE_BRONZE", "PB_MACHINE_SILVER"),
        "WIND_WARRIOR" to listOf("WIND_WARRIOR_BRONZE", "WIND_WARRIOR_SILVER", "WIND_WARRIOR_GOLD"),
        "SNIPER" to listOf("SNIPER_BRONZE", "SNIPER_SILVER", "SNIPER_GOLD"),
        "HOT_STREAK" to listOf("HOT_STREAK_BRONZE", "HOT_STREAK_SILVER"),
        "WEATHERPROOF" to listOf("WEATHERPROOF_BRONZE", "WEATHERPROOF_SILVER"),
        "IRON_MAN" to listOf("IRON_MAN_BRONZE", "IRON_MAN_SILVER", "IRON_MAN_GOLD"),
        "DAWN_PATROL" to listOf("DAWN_PATROL_BRONZE"),
        "NIGHT_OWL" to listOf("NIGHT_OWL_BRONZE")
    )

    fun migrateOldKeys() {
        val currentVersion = prefs.getInt("achievement_migration_version", 0)
        if (currentVersion >= 1) return

        val current = loadUnlocked().toMutableMap()
        var migrated = false

        for ((oldKey, newKeys) in oldToNewMapping) {
            val ts = current[oldKey]
            if (ts != null) {
                for (newKey in newKeys) {
                    if (newKey !in current) {
                        current[newKey] = ts
                        migrated = true
                    }
                }
                current.remove(oldKey)
                migrated = true
            }
        }

        if (migrated) {
            saveUnlocked(current)
        }
        prefs.edit().putInt("achievement_migration_version", 1).apply()
    }

    // ── Local persistence ────────────────────────────────────────────────────

    fun loadUnlocked(): Map<String, Long> {
        val json = prefs.getString("unlocked", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Long>()
            obj.keys().forEach { key -> map[key] = obj.getLong(key) }
            map
        } catch (e: Exception) {
            Log.e("AchievementRepo", "Failed to load achievements", e)
            emptyMap()
        }
    }

    fun saveUnlocked(map: Map<String, Long>) {
        val obj = JSONObject()
        map.forEach { (key, ts) -> obj.put(key, ts) }
        prefs.edit().putString("unlocked", obj.toString()).apply()
    }

    // ── Firestore persistence ────────────────────────────────────────────────

    suspend fun saveToFirestore(storageKey: String, timestampMs: Long, forUid: String?) {
        val currentUid = forUid ?: uid ?: return
        firestore.collection("users").document(currentUid)
            .collection("achievements").document(storageKey)
            .set(mapOf("name" to storageKey, "unlockedAt" to timestampMs))
            .await()
    }

    fun achievementsFlow(): Flow<Map<String, Long>> {
        val currentUid = uid ?: return flowOf(loadUnlocked())
        return callbackFlow {
            val registration = firestore.collection("users").document(currentUid)
                .collection("achievements")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AchievementRepo", "Firestore listener error", error)
                        trySend(loadUnlocked())
                        return@addSnapshotListener
                    }
                    val map = mutableMapOf<String, Long>()
                    snapshot?.documents?.forEach { doc ->
                        val name = doc.getString("name") ?: doc.id
                        val ts = doc.getLong("unlockedAt") ?: 0L
                        map[name] = ts
                    }
                    trySend(map)
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun migrateLocalToFirestore() {
        val currentUid = uid ?: return
        val local = loadUnlocked()
        if (local.isEmpty()) return
        val batch = firestore.batch()
        local.forEach { (name, ts) ->
            val ref = firestore.collection("users").document(currentUid)
                .collection("achievements").document(name)
            batch.set(ref, mapOf("name" to name, "unlockedAt" to ts), SetOptions.merge())
        }
        batch.commit().await()
    }
}
