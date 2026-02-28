package com.smacktrack.golf.domain

import com.smacktrack.golf.ui.ShotResult
import java.util.Calendar

/**
 * Pure function that checks which achievements were newly unlocked by [newShot].
 *
 * [allShots] must already include [newShot]. Returns only achievements not in [alreadyUnlocked].
 */
fun checkAchievements(
    allShots: List<ShotResult>,
    newShot: ShotResult,
    alreadyUnlocked: Set<String>,
    enabledClubs: Set<Club>
): List<Achievement> {
    val newlyUnlocked = mutableListOf<Achievement>()

    fun check(a: Achievement, condition: () -> Boolean) {
        if (a.name !in alreadyUnlocked && condition()) newlyUnlocked.add(a)
    }

    // FIRST_BLOOD — first shot ever
    check(Achievement.FIRST_BLOOD) { allShots.size == 1 }

    // CLUB_250 — Driver over 250 yards
    check(Achievement.CLUB_250) {
        newShot.club == Club.DRIVER && newShot.distanceYards >= 250
    }

    // CENTURY — 100 total shots
    check(Achievement.CENTURY) { allShots.size >= 100 }

    // FULL_BAG — every enabled club used at least once
    check(Achievement.FULL_BAG) {
        enabledClubs.isNotEmpty() && enabledClubs.all { club ->
            allShots.any { it.club == club }
        }
    }

    // PB_MACHINE — beat PB 3 times in a month (any club)
    check(Achievement.PB_MACHINE) {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        var pbBreaks = 0
        val clubGroups = allShots.groupBy { it.club }
        for ((_, shots) in clubGroups) {
            val sorted = shots.sortedBy { it.timestampMs }
            var currentPb = 0
            for (shot in sorted) {
                if (shot.distanceYards > currentPb) {
                    // Count as PB break only if it's a genuine improvement AND within last 30 days
                    if (currentPb > 0 && shot.timestampMs >= thirtyDaysAgo) pbBreaks++
                    currentPb = shot.distanceYards
                }
            }
        }
        pbBreaks >= 3
    }

    // WIND_WARRIOR — shot in 30+ km/h wind
    check(Achievement.WIND_WARRIOR) { newShot.windSpeedKmh >= 30.0 }

    // SNIPER — 5 consecutive same-club shots within 10 yd spread
    check(Achievement.SNIPER) {
        val clubShots = allShots.filter { it.club == newShot.club }
            .sortedBy { it.timestampMs }
            .takeLast(5)
        if (clubShots.size >= 5) {
            val spread = clubShots.maxOf { it.distanceYards } - clubShots.minOf { it.distanceYards }
            spread <= 10
        } else false
    }

    // HOT_STREAK — 3 shots in a row above club average
    check(Achievement.HOT_STREAK) {
        val lastThree = allShots.sortedBy { it.timestampMs }.takeLast(3)
        if (lastThree.size >= 3) {
            lastThree.all { shot ->
                val clubShots = allShots.filter { it.club == shot.club && it.timestampMs != shot.timestampMs }
                if (clubShots.isEmpty()) false
                else shot.distanceYards > clubShots.map { it.distanceYards }.average()
            }
        } else false
    }

    // WEATHERPROOF — shots in 3+ different weather conditions
    check(Achievement.WEATHERPROOF) {
        val conditions = allShots.map { it.weatherDescription }.distinct()
            .filter { it.isNotBlank() && it != "Unknown" }
        conditions.size >= 3
    }

    // IRON_MAN — 50 shots with irons
    check(Achievement.IRON_MAN) {
        allShots.count { it.club.category == Club.Category.IRON } >= 50
    }

    // DAWN_PATROL — shot before 7 AM
    check(Achievement.DAWN_PATROL) {
        val cal = Calendar.getInstance().apply { timeInMillis = newShot.timestampMs }
        cal.get(Calendar.HOUR_OF_DAY) < 7
    }

    // NIGHT_OWL — shot after 8 PM
    check(Achievement.NIGHT_OWL) {
        val cal = Calendar.getInstance().apply { timeInMillis = newShot.timestampMs }
        cal.get(Calendar.HOUR_OF_DAY) >= 20
    }

    return newlyUnlocked
}
