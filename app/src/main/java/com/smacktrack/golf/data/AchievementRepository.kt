package com.smacktrack.golf.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.smacktrack.golf.domain.Achievement
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

    suspend fun saveToFirestore(achievement: Achievement, timestampMs: Long, forUid: String?) {
        val currentUid = forUid ?: uid ?: return
        firestore.collection("users").document(currentUid)
            .collection("achievements").document(achievement.name)
            .set(mapOf("name" to achievement.name, "unlockedAt" to timestampMs))
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
            // Merge so we don't overwrite achievements earned on other devices
            batch.set(ref, mapOf("name" to name, "unlockedAt" to ts), SetOptions.merge())
        }
        batch.commit().await()
    }
}
