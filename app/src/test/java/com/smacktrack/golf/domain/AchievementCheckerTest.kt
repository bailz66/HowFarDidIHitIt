package com.smacktrack.golf.domain

import com.smacktrack.golf.ui.ShotResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Calendar

@DisplayName("AchievementChecker tests")
class AchievementCheckerTest {

    private fun shot(
        club: Club = Club.SEVEN_IRON,
        yards: Int = 150,
        windKmh: Double = 10.0,
        weather: String = "Clear",
        timestampMs: Long = System.currentTimeMillis()
    ) = ShotResult(
        club = club,
        distanceYards = yards,
        distanceMeters = (yards * 0.9144).toInt(),
        weatherDescription = weather,
        temperatureF = 72,
        temperatureC = 22,
        windSpeedKmh = windKmh,
        windDirectionCompass = "N",
        timestampMs = timestampMs
    )

    private val allClubs = Club.entries.toSet()

    @Test
    @DisplayName("FIRST_BLOOD unlocks on first shot")
    fun firstBlood() {
        val newShot = shot()
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue(result.contains(Achievement.FIRST_BLOOD))
    }

    @Test
    @DisplayName("FIRST_BLOOD does not unlock on second shot")
    fun firstBloodNotOnSecond() {
        val shot1 = shot(timestampMs = 1000)
        val shot2 = shot(timestampMs = 2000)
        val result = checkAchievements(listOf(shot1, shot2), shot2, emptySet(), allClubs)
        assertTrue(!result.contains(Achievement.FIRST_BLOOD))
    }

    @Test
    @DisplayName("CLUB_250 unlocks for Driver over 250 yards")
    fun club250() {
        val newShot = shot(club = Club.DRIVER, yards = 260)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue(result.contains(Achievement.CLUB_250))
    }

    @Test
    @DisplayName("CLUB_250 does not unlock for non-Driver")
    fun club250NotIron() {
        val newShot = shot(club = Club.SEVEN_IRON, yards = 260)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue(!result.contains(Achievement.CLUB_250))
    }

    @Test
    @DisplayName("CENTURY unlocks at 100 shots")
    fun century() {
        val shots = (1..100).map { shot(timestampMs = it.toLong()) }
        val newShot = shots.last()
        val result = checkAchievements(shots, newShot, emptySet(), allClubs)
        assertTrue(result.contains(Achievement.CENTURY))
    }

    @Test
    @DisplayName("CENTURY does not unlock at 99 shots")
    fun centuryNot99() {
        val shots = (1..99).map { shot(timestampMs = it.toLong()) }
        val newShot = shots.last()
        val result = checkAchievements(shots, newShot, emptySet(), allClubs)
        assertTrue(!result.contains(Achievement.CENTURY))
    }

    @Test
    @DisplayName("WIND_WARRIOR unlocks at 30+ km/h wind")
    fun windWarrior() {
        val newShot = shot(windKmh = 35.0)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue(result.contains(Achievement.WIND_WARRIOR))
    }

    @Test
    @DisplayName("WIND_WARRIOR does not unlock below 30 km/h")
    fun windWarriorNot() {
        val newShot = shot(windKmh = 25.0)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue(!result.contains(Achievement.WIND_WARRIOR))
    }

    @Test
    @DisplayName("WEATHERPROOF unlocks with 3 different conditions")
    fun weatherproof() {
        val shots = listOf(
            shot(weather = "Clear", timestampMs = 1000),
            shot(weather = "Cloudy", timestampMs = 2000),
            shot(weather = "Rain", timestampMs = 3000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        assertTrue(result.contains(Achievement.WEATHERPROOF))
    }

    @Test
    @DisplayName("IRON_MAN unlocks with 50 iron shots")
    fun ironMan() {
        val ironClubs = Club.entries.filter { it.category == Club.Category.IRON }
        val shots = (1..50).map { shot(club = ironClubs[it % ironClubs.size], timestampMs = it.toLong()) }
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        assertTrue(result.contains(Achievement.IRON_MAN))
    }

    @Test
    @DisplayName("DAWN_PATROL unlocks before 7 AM")
    fun dawnPatrol() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 30)
        }
        val newShot = shot(timestampMs = cal.timeInMillis)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue(result.contains(Achievement.DAWN_PATROL))
    }

    @Test
    @DisplayName("NIGHT_OWL unlocks after 8 PM")
    fun nightOwl() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
        }
        val newShot = shot(timestampMs = cal.timeInMillis)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue(result.contains(Achievement.NIGHT_OWL))
    }

    @Test
    @DisplayName("Already unlocked achievements are skipped")
    fun alreadyUnlockedSkipped() {
        val newShot = shot()
        val alreadyUnlocked = setOf(Achievement.FIRST_BLOOD.name)
        val result = checkAchievements(listOf(newShot), newShot, alreadyUnlocked, allClubs)
        assertTrue(!result.contains(Achievement.FIRST_BLOOD))
    }

    @Test
    @DisplayName("SNIPER unlocks with 5 same-club shots within 10 yd spread")
    fun sniper() {
        val shots = listOf(
            shot(club = Club.SEVEN_IRON, yards = 150, timestampMs = 1000),
            shot(club = Club.SEVEN_IRON, yards = 152, timestampMs = 2000),
            shot(club = Club.SEVEN_IRON, yards = 148, timestampMs = 3000),
            shot(club = Club.SEVEN_IRON, yards = 155, timestampMs = 4000),
            shot(club = Club.SEVEN_IRON, yards = 153, timestampMs = 5000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        assertTrue(result.contains(Achievement.SNIPER))
    }

    @Test
    @DisplayName("FULL_BAG unlocks when all enabled clubs used")
    fun fullBag() {
        val enabledClubs = setOf(Club.DRIVER, Club.SEVEN_IRON, Club.PITCHING_WEDGE)
        val shots = listOf(
            shot(club = Club.DRIVER, timestampMs = 1000),
            shot(club = Club.SEVEN_IRON, timestampMs = 2000),
            shot(club = Club.PITCHING_WEDGE, timestampMs = 3000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), enabledClubs)
        assertTrue(result.contains(Achievement.FULL_BAG))
    }

    @Test
    @DisplayName("FULL_BAG does not unlock when a club is missing")
    fun fullBagMissing() {
        val enabledClubs = setOf(Club.DRIVER, Club.SEVEN_IRON, Club.PITCHING_WEDGE)
        val shots = listOf(
            shot(club = Club.DRIVER, timestampMs = 1000),
            shot(club = Club.SEVEN_IRON, timestampMs = 2000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), enabledClubs)
        assertTrue(!result.contains(Achievement.FULL_BAG))
    }

    @Test
    @DisplayName("HOT_STREAK unlocks with 3 shots above club average")
    fun hotStreak() {
        // Build history: 10 mediocre shots + 3 hot shots
        val baseShots = (1..10).map { shot(yards = 140, timestampMs = it.toLong()) }
        val hotShots = listOf(
            shot(yards = 170, timestampMs = 11),
            shot(yards = 175, timestampMs = 12),
            shot(yards = 180, timestampMs = 13)
        )
        val allShots = baseShots + hotShots
        val result = checkAchievements(allShots, hotShots.last(), emptySet(), allClubs)
        assertTrue(result.contains(Achievement.HOT_STREAK))
    }

    @Test
    @DisplayName("PB_MACHINE unlocks with 3 PB breaks in 30 days")
    fun pbMachine() {
        val now = System.currentTimeMillis()
        val shots = listOf(
            shot(club = Club.DRIVER, yards = 200, timestampMs = now - 25L * 24 * 60 * 60 * 1000), // baseline
            shot(club = Club.DRIVER, yards = 210, timestampMs = now - 20L * 24 * 60 * 60 * 1000), // PB break 1
            shot(club = Club.DRIVER, yards = 220, timestampMs = now - 15L * 24 * 60 * 60 * 1000), // PB break 2
            shot(club = Club.DRIVER, yards = 230, timestampMs = now - 10L * 24 * 60 * 60 * 1000), // PB break 3
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        assertTrue(result.contains(Achievement.PB_MACHINE))
    }
}
