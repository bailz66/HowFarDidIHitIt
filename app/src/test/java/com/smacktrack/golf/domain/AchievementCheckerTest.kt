package com.smacktrack.golf.domain

import com.smacktrack.golf.ui.ShotResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Calendar

@DisplayName("AchievementChecker tiered tests")
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

    private fun storageKeys(result: List<UnlockedAchievement>): Set<String> =
        result.map { it.storageKey }.toSet()

    // ── SHOT_COUNT ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("SHOT_COUNT Bronze unlocks on first shot")
    fun shotCountBronze() {
        val newShot = shot()
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue("SHOT_COUNT_BRONZE" in storageKeys(result))
    }

    @Test
    @DisplayName("SHOT_COUNT Silver unlocks at 25 shots")
    fun shotCountSilver() {
        val shots = (1..25).map { shot(timestampMs = it.toLong()) }
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        val keys = storageKeys(result)
        assertTrue("SHOT_COUNT_BRONZE" in keys)
        assertTrue("SHOT_COUNT_SILVER" in keys)
    }

    @Test
    @DisplayName("SHOT_COUNT Gold unlocks at 100 shots with Bronze+Silver backfill")
    fun shotCountGoldBackfill() {
        val shots = (1..100).map { shot(timestampMs = it.toLong()) }
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        val keys = storageKeys(result)
        assertTrue("SHOT_COUNT_BRONZE" in keys)
        assertTrue("SHOT_COUNT_SILVER" in keys)
        assertTrue("SHOT_COUNT_GOLD" in keys)
    }

    // ── BOMBER ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BOMBER Bronze unlocks for Driver over 200 yards")
    fun bomberBronze() {
        val newShot = shot(club = Club.DRIVER, yards = 210)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue("BOMBER_BRONZE" in storageKeys(result))
    }

    @Test
    @DisplayName("BOMBER Gold unlocks for Driver over 250 yards, backfills Bronze+Silver")
    fun bomberGoldBackfill() {
        val newShot = shot(club = Club.DRIVER, yards = 260)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        val keys = storageKeys(result)
        assertTrue("BOMBER_BRONZE" in keys)
        assertTrue("BOMBER_SILVER" in keys)
        assertTrue("BOMBER_GOLD" in keys)
    }

    @Test
    @DisplayName("BOMBER does not unlock for non-Driver")
    fun bomberNotIron() {
        val newShot = shot(club = Club.SEVEN_IRON, yards = 260)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue("BOMBER_BRONZE" !in storageKeys(result))
    }

    // ── FULL_BAG ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FULL_BAG Diamond unlocks when all enabled clubs used")
    fun fullBagDiamond() {
        val enabledClubs = setOf(Club.DRIVER, Club.SEVEN_IRON, Club.PITCHING_WEDGE)
        val shots = listOf(
            shot(club = Club.DRIVER, timestampMs = 1000),
            shot(club = Club.SEVEN_IRON, timestampMs = 2000),
            shot(club = Club.PITCHING_WEDGE, timestampMs = 3000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), enabledClubs)
        val keys = storageKeys(result)
        assertTrue("FULL_BAG_BRONZE" in keys)
        assertTrue("FULL_BAG_DIAMOND" in keys)
    }

    @Test
    @DisplayName("FULL_BAG does not unlock Diamond when a club is missing")
    fun fullBagMissingClub() {
        val enabledClubs = setOf(Club.DRIVER, Club.SEVEN_IRON, Club.PITCHING_WEDGE)
        val shots = listOf(
            shot(club = Club.DRIVER, timestampMs = 1000),
            shot(club = Club.SEVEN_IRON, timestampMs = 2000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), enabledClubs)
        assertTrue("FULL_BAG_DIAMOND" !in storageKeys(result))
    }

    // ── WIND_WARRIOR ────────────────────────────────────────────────────────

    @Test
    @DisplayName("WIND_WARRIOR Gold unlocks at 30+ km/h, backfills Bronze+Silver")
    fun windWarriorGold() {
        val newShot = shot(windKmh = 35.0)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        val keys = storageKeys(result)
        assertTrue("WIND_WARRIOR_BRONZE" in keys)
        assertTrue("WIND_WARRIOR_SILVER" in keys)
        assertTrue("WIND_WARRIOR_GOLD" in keys)
    }

    @Test
    @DisplayName("WIND_WARRIOR Bronze does not unlock below 15 km/h")
    fun windWarriorNotUnder15() {
        val newShot = shot(windKmh = 12.0)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue("WIND_WARRIOR_BRONZE" !in storageKeys(result))
    }

    // ── WEATHERPROOF ────────────────────────────────────────────────────────

    @Test
    @DisplayName("WEATHERPROOF Silver unlocks with 3 different conditions")
    fun weatherproofSilver() {
        val shots = listOf(
            shot(weather = "Clear", timestampMs = 1000),
            shot(weather = "Cloudy", timestampMs = 2000),
            shot(weather = "Rain", timestampMs = 3000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        val keys = storageKeys(result)
        assertTrue("WEATHERPROOF_BRONZE" in keys)
        assertTrue("WEATHERPROOF_SILVER" in keys)
    }

    // ── IRON_MAN ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("IRON_MAN Gold unlocks at 50 iron shots")
    fun ironManGold() {
        val ironClubs = Club.entries.filter { it.category == Club.Category.IRON }
        val shots = (1..50).map { shot(club = ironClubs[it % ironClubs.size], timestampMs = it.toLong()) }
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        val keys = storageKeys(result)
        assertTrue("IRON_MAN_BRONZE" in keys)
        assertTrue("IRON_MAN_SILVER" in keys)
        assertTrue("IRON_MAN_GOLD" in keys)
    }

    // ── DAWN_PATROL ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("DAWN_PATROL Bronze unlocks before 7 AM")
    fun dawnPatrolBronze() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 30)
        }
        val newShot = shot(timestampMs = cal.timeInMillis)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue("DAWN_PATROL_BRONZE" in storageKeys(result))
    }

    // ── NIGHT_OWL ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("NIGHT_OWL Bronze unlocks after 8 PM")
    fun nightOwlBronze() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
        }
        val newShot = shot(timestampMs = cal.timeInMillis)
        val result = checkAchievements(listOf(newShot), newShot, emptySet(), allClubs)
        assertTrue("NIGHT_OWL_BRONZE" in storageKeys(result))
    }

    // ── Already unlocked skip ───────────────────────────────────────────────

    @Test
    @DisplayName("Already unlocked achievements are skipped")
    fun alreadyUnlockedSkipped() {
        val newShot = shot()
        val alreadyUnlocked = setOf("SHOT_COUNT_BRONZE")
        val result = checkAchievements(listOf(newShot), newShot, alreadyUnlocked, allClubs)
        assertTrue("SHOT_COUNT_BRONZE" !in storageKeys(result))
    }

    // ── Auto-backfill ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Auto-backfill only emits missing tiers")
    fun backfillSkipsAlreadyEarned() {
        val shots = (1..100).map { shot(timestampMs = it.toLong()) }
        // Bronze already earned
        val alreadyUnlocked = setOf("SHOT_COUNT_BRONZE")
        val result = checkAchievements(shots, shots.last(), alreadyUnlocked, allClubs)
        val keys = storageKeys(result)
        assertTrue("SHOT_COUNT_BRONZE" !in keys) // already had it
        assertTrue("SHOT_COUNT_SILVER" in keys)
        assertTrue("SHOT_COUNT_GOLD" in keys)
    }

    // ── SNIPER ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SNIPER Bronze unlocks with 3 same-club shots within 15 yd spread")
    fun sniperBronze() {
        val shots = listOf(
            shot(club = Club.SEVEN_IRON, yards = 150, timestampMs = 1000),
            shot(club = Club.SEVEN_IRON, yards = 152, timestampMs = 2000),
            shot(club = Club.SEVEN_IRON, yards = 148, timestampMs = 3000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        assertTrue("SNIPER_BRONZE" in storageKeys(result))
    }

    @Test
    @DisplayName("SNIPER Gold unlocks with 5 same-club shots within 10 yd spread")
    fun sniperGold() {
        val shots = listOf(
            shot(club = Club.SEVEN_IRON, yards = 150, timestampMs = 1000),
            shot(club = Club.SEVEN_IRON, yards = 152, timestampMs = 2000),
            shot(club = Club.SEVEN_IRON, yards = 148, timestampMs = 3000),
            shot(club = Club.SEVEN_IRON, yards = 155, timestampMs = 4000),
            shot(club = Club.SEVEN_IRON, yards = 153, timestampMs = 5000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        val keys = storageKeys(result)
        assertTrue("SNIPER_BRONZE" in keys)
        assertTrue("SNIPER_SILVER" in keys)
        assertTrue("SNIPER_GOLD" in keys)
    }

    // ── HOT_STREAK ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("HOT_STREAK Silver unlocks with 3 consecutive above club avg")
    fun hotStreakSilver() {
        val baseShots = (1..10).map { shot(yards = 140, timestampMs = it.toLong()) }
        val hotShots = listOf(
            shot(yards = 170, timestampMs = 11),
            shot(yards = 175, timestampMs = 12),
            shot(yards = 180, timestampMs = 13)
        )
        val allShots = baseShots + hotShots
        val result = checkAchievements(allShots, hotShots.last(), emptySet(), allClubs)
        val keys = storageKeys(result)
        assertTrue("HOT_STREAK_BRONZE" in keys)
        assertTrue("HOT_STREAK_SILVER" in keys)
    }

    // ── PB_MACHINE ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("PB_MACHINE Silver unlocks with 3 all-time PB breaks")
    fun pbMachineSilver() {
        val shots = listOf(
            shot(club = Club.DRIVER, yards = 200, timestampMs = 1000),
            shot(club = Club.DRIVER, yards = 210, timestampMs = 2000),
            shot(club = Club.DRIVER, yards = 220, timestampMs = 3000),
            shot(club = Club.DRIVER, yards = 230, timestampMs = 4000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        val keys = storageKeys(result)
        assertTrue("PB_MACHINE_BRONZE" in keys)
        assertTrue("PB_MACHINE_SILVER" in keys)
    }

    // ── DEDICATED ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("DEDICATED Bronze unlocks with 3 sessions")
    fun dedicatedBronze() {
        val gap = 31 * 60 * 1000L // 31 min gap
        val shots = listOf(
            shot(timestampMs = 1000),
            shot(timestampMs = 1000 + gap),
            shot(timestampMs = 1000 + gap * 2)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        assertTrue("DEDICATED_BRONZE" in storageKeys(result))
    }

    @Test
    @DisplayName("DEDICATED does not unlock without enough sessions")
    fun dedicatedNotEnough() {
        // All shots within one session
        val shots = listOf(
            shot(timestampMs = 1000),
            shot(timestampMs = 2000),
            shot(timestampMs = 3000)
        )
        val result = checkAchievements(shots, shots.last(), emptySet(), allClubs)
        assertTrue("DEDICATED_BRONZE" !in storageKeys(result))
    }

    // ── UnlockedAchievement.fromStorageKey ───────────────────────────────────

    @Test
    @DisplayName("fromStorageKey parses valid keys")
    fun fromStorageKeyValid() {
        val ua = UnlockedAchievement.fromStorageKey("BOMBER_GOLD")!!
        assertEquals(AchievementCategory.BOMBER, ua.category)
        assertEquals(AchievementTier.GOLD, ua.tier)
    }

    @Test
    @DisplayName("fromStorageKey handles compound category names")
    fun fromStorageKeyCompound() {
        val ua = UnlockedAchievement.fromStorageKey("SHOT_COUNT_DIAMOND")!!
        assertEquals(AchievementCategory.SHOT_COUNT, ua.category)
        assertEquals(AchievementTier.DIAMOND, ua.tier)
    }

    @Test
    @DisplayName("fromStorageKey returns null for invalid keys")
    fun fromStorageKeyInvalid() {
        assertTrue(UnlockedAchievement.fromStorageKey("INVALID") == null)
        assertTrue(UnlockedAchievement.fromStorageKey("BOMBER_INVALID") == null)
    }
}
