package com.smacktrack.golf.domain

import com.smacktrack.golf.ui.ShotResult
import java.util.Calendar

private const val SESSION_GAP_MS = 30 * 60 * 1000L

/**
 * Pure function that checks which tiered achievements were newly unlocked by [newShot].
 *
 * [allShots] must already include [newShot]. Returns only achievements not in [alreadyUnlocked].
 */
fun checkAchievements(
    allShots: List<ShotResult>,
    newShot: ShotResult,
    alreadyUnlocked: Set<String>,
    enabledClubs: Set<Club>
): List<UnlockedAchievement> {
    val newlyUnlocked = mutableListOf<UnlockedAchievement>()

    fun unlockUpTo(category: AchievementCategory, highestTierIndex: Int) {
        val tiers = AchievementTier.entries
        for (i in 0..highestTierIndex) {
            val ua = UnlockedAchievement(category, tiers[i])
            if (ua.storageKey !in alreadyUnlocked) {
                newlyUnlocked.add(ua)
            }
        }
    }

    // SHOT_COUNT — total number of shots
    run {
        val count = allShots.size
        val cat = AchievementCategory.SHOT_COUNT
        val highest = cat.tiers.indexOfLast { count >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // BOMBER — max driver distance in yards
    run {
        val cat = AchievementCategory.BOMBER
        val maxDriverYards = allShots
            .filter { it.club == Club.DRIVER }
            .maxOfOrNull { it.distanceYards } ?: 0
        val highest = cat.tiers.indexOfLast { maxDriverYards >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // FULL_BAG — number of distinct clubs used (Diamond = all enabled)
    run {
        val cat = AchievementCategory.FULL_BAG
        val clubsUsed = allShots.map { it.club }.toSet()
        val distinctCount = clubsUsed.size
        var highest = -1
        for (i in cat.tiers.indices) {
            val tier = cat.tiers[i]
            if (tier.threshold == -1) {
                // Diamond: all enabled clubs used at least once
                if (enabledClubs.isNotEmpty() && enabledClubs.all { it in clubsUsed }) {
                    highest = i
                }
            } else if (distinctCount >= tier.threshold) {
                highest = i
            }
        }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // PB_MACHINE — all-time PB break count (no time window)
    run {
        val cat = AchievementCategory.PB_MACHINE
        var pbBreaks = 0
        val clubGroups = allShots.groupBy { it.club }
        for ((_, shots) in clubGroups) {
            val sorted = shots.sortedBy { it.timestampMs }
            var currentPb = 0
            for (shot in sorted) {
                if (shot.distanceYards > currentPb) {
                    if (currentPb > 0) pbBreaks++
                    currentPb = shot.distanceYards
                }
            }
        }
        val highest = cat.tiers.indexOfLast { pbBreaks >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // WIND_WARRIOR — max wind speed across all shots
    run {
        val cat = AchievementCategory.WIND_WARRIOR
        val maxWind = allShots.maxOfOrNull { it.windSpeedKmh } ?: 0.0
        val highest = cat.tiers.indexOfLast { maxWind >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // SNIPER — dual threshold (count + spread) per tier
    run {
        val cat = AchievementCategory.SNIPER
        // Check last N same-club shots for each tier
        val clubShots = allShots.filter { it.club == newShot.club }
            .sortedBy { it.timestampMs }
        var highest = -1
        for (i in cat.tiers.indices) {
            val tier = cat.tiers[i]
            val requiredCount = tier.threshold
            val maxSpread = tier.secondaryThreshold
            if (clubShots.size >= requiredCount) {
                val recent = clubShots.takeLast(requiredCount)
                val spread = recent.maxOf { it.distanceYards } - recent.minOf { it.distanceYards }
                if (spread <= maxSpread) {
                    highest = i
                }
            }
        }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // HOT_STREAK — longest consecutive streak above club avg (excluding own shot from avg)
    run {
        val cat = AchievementCategory.HOT_STREAK
        val sorted = allShots.sortedBy { it.timestampMs }
        var longestStreak = 0
        var currentStreak = 0
        for (shot in sorted) {
            val clubOthers = allShots.filter { it.club == shot.club && it.timestampMs != shot.timestampMs }
            val aboveAvg = if (clubOthers.isEmpty()) false
            else shot.distanceYards > clubOthers.map { it.distanceYards }.average()
            if (aboveAvg) {
                currentStreak++
                if (currentStreak > longestStreak) longestStreak = currentStreak
            } else {
                currentStreak = 0
            }
        }
        val highest = cat.tiers.indexOfLast { longestStreak >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // WEATHERPROOF — distinct weather conditions
    run {
        val cat = AchievementCategory.WEATHERPROOF
        val conditions = allShots.map { it.weatherDescription }.distinct()
            .filter { it.isNotBlank() && it != "Unknown" }
        val highest = cat.tiers.indexOfLast { conditions.size >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // IRON_MAN — total iron shots
    run {
        val cat = AchievementCategory.IRON_MAN
        val ironCount = allShots.count { it.club.category == Club.Category.IRON }
        val highest = cat.tiers.indexOfLast { ironCount >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // DAWN_PATROL — shots before 7 AM
    run {
        val cat = AchievementCategory.DAWN_PATROL
        val earlyCount = allShots.count { shot ->
            val cal = Calendar.getInstance().apply { timeInMillis = shot.timestampMs }
            cal.get(Calendar.HOUR_OF_DAY) < 7
        }
        val highest = cat.tiers.indexOfLast { earlyCount >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // NIGHT_OWL — shots after 8 PM
    run {
        val cat = AchievementCategory.NIGHT_OWL
        val lateCount = allShots.count { shot ->
            val cal = Calendar.getInstance().apply { timeInMillis = shot.timestampMs }
            cal.get(Calendar.HOUR_OF_DAY) >= 20
        }
        val highest = cat.tiers.indexOfLast { lateCount >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    // DEDICATED — session count (30-min gap between sessions)
    run {
        val cat = AchievementCategory.DEDICATED
        val sessionCount = countSessions(allShots)
        val highest = cat.tiers.indexOfLast { sessionCount >= it.threshold }
        if (highest >= 0) unlockUpTo(cat, highest)
    }

    return newlyUnlocked
}

private fun countSessions(shots: List<ShotResult>): Int {
    if (shots.isEmpty()) return 0
    val sorted = shots.sortedBy { it.timestampMs }
    var count = 1
    for (i in 1 until sorted.size) {
        if (sorted[i].timestampMs - sorted[i - 1].timestampMs > SESSION_GAP_MS) {
            count++
        }
    }
    return count
}
