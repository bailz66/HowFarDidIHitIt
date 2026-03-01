package com.smacktrack.golf.domain

enum class AchievementTier { BRONZE, SILVER, GOLD, PLATINUM, DIAMOND }

data class TierDef(
    val threshold: Int,
    val description: String,
    val secondaryThreshold: Int = 0
)

enum class AchievementCategory(
    val displayName: String,
    val icon: String,
    val tiers: List<TierDef>
) {
    SHOT_COUNT(
        "Shot Count", "\uD83C\uDFAF",
        listOf(
            TierDef(1, "Record your first shot"),
            TierDef(25, "Track 25 shots"),
            TierDef(100, "Track 100 shots"),
            TierDef(250, "Track 250 shots"),
            TierDef(500, "Track 500 shots")
        )
    ),
    BOMBER(
        "Bomber", "\uD83D\uDCA3",
        listOf(
            TierDef(200, "Driver over 200 yards"),
            TierDef(225, "Driver over 225 yards"),
            TierDef(250, "Driver over 250 yards"),
            TierDef(275, "Driver over 275 yards"),
            TierDef(300, "Driver over 300 yards")
        )
    ),
    FULL_BAG(
        "Full Bag", "\uD83C\uDFCC\uFE0F",
        listOf(
            TierDef(3, "Use 3 different clubs"),
            TierDef(5, "Use 5 different clubs"),
            TierDef(8, "Use 8 different clubs"),
            TierDef(12, "Use 12 different clubs"),
            TierDef(-1, "Use every enabled club")
        )
    ),
    PB_MACHINE(
        "PB Machine", "\uD83D\uDE80",
        listOf(
            TierDef(1, "Beat your PB once"),
            TierDef(3, "Beat your PB 3 times"),
            TierDef(10, "Beat your PB 10 times"),
            TierDef(25, "Beat your PB 25 times"),
            TierDef(50, "Beat your PB 50 times")
        )
    ),
    WIND_WARRIOR(
        "Wind Warrior", "\uD83C\uDF2C\uFE0F",
        listOf(
            TierDef(15, "Shot in 15+ km/h wind"),
            TierDef(20, "Shot in 20+ km/h wind"),
            TierDef(30, "Shot in 30+ km/h wind"),
            TierDef(40, "Shot in 40+ km/h wind"),
            TierDef(50, "Shot in 50+ km/h wind")
        )
    ),
    SNIPER(
        "Sniper", "\uD83C\uDFF9",
        listOf(
            TierDef(3, "3 same-club shots within 15 yds", secondaryThreshold = 15),
            TierDef(5, "5 same-club shots within 15 yds", secondaryThreshold = 15),
            TierDef(5, "5 same-club shots within 10 yds", secondaryThreshold = 10),
            TierDef(7, "7 same-club shots within 10 yds", secondaryThreshold = 10),
            TierDef(10, "10 same-club shots within 10 yds", secondaryThreshold = 10)
        )
    ),
    HOT_STREAK(
        "Hot Streak", "\uD83D\uDD25",
        listOf(
            TierDef(2, "2 consecutive above club avg"),
            TierDef(3, "3 consecutive above club avg"),
            TierDef(5, "5 consecutive above club avg"),
            TierDef(7, "7 consecutive above club avg"),
            TierDef(10, "10 consecutive above club avg")
        )
    ),
    WEATHERPROOF(
        "Weatherproof", "\u2614",
        listOf(
            TierDef(2, "Shots in 2 weather conditions"),
            TierDef(3, "Shots in 3 weather conditions"),
            TierDef(4, "Shots in 4 weather conditions"),
            TierDef(5, "Shots in 5 weather conditions"),
            TierDef(6, "Shots in 6 weather conditions")
        )
    ),
    IRON_MAN(
        "Iron Man", "\uD83E\uDDBE",
        listOf(
            TierDef(10, "10 shots with irons"),
            TierDef(25, "25 shots with irons"),
            TierDef(50, "50 shots with irons"),
            TierDef(100, "100 shots with irons"),
            TierDef(200, "200 shots with irons")
        )
    ),
    DAWN_PATROL(
        "Dawn Patrol", "\uD83C\uDF05",
        listOf(
            TierDef(1, "1 shot before 7 AM"),
            TierDef(5, "5 shots before 7 AM"),
            TierDef(10, "10 shots before 7 AM"),
            TierDef(25, "25 shots before 7 AM"),
            TierDef(50, "50 shots before 7 AM")
        )
    ),
    NIGHT_OWL(
        "Night Owl", "\uD83C\uDF19",
        listOf(
            TierDef(1, "1 shot after 8 PM"),
            TierDef(5, "5 shots after 8 PM"),
            TierDef(10, "10 shots after 8 PM"),
            TierDef(25, "25 shots after 8 PM"),
            TierDef(50, "50 shots after 8 PM")
        )
    ),
    DEDICATED(
        "Dedicated", "\uD83D\uDCC5",
        listOf(
            TierDef(3, "3 practice sessions"),
            TierDef(10, "10 practice sessions"),
            TierDef(25, "25 practice sessions"),
            TierDef(50, "50 practice sessions"),
            TierDef(100, "100 practice sessions")
        )
    );

    companion object {
        const val TOTAL = 60 // 12 categories × 5 tiers
    }
}

data class UnlockedAchievement(
    val category: AchievementCategory,
    val tier: AchievementTier
) {
    val storageKey: String = "${category.name}_${tier.name}"

    companion object {
        fun fromStorageKey(key: String): UnlockedAchievement? {
            val parts = key.split("_")
            if (parts.size < 2) return null
            // Tier is always the last part; category is everything before it
            val tierName = parts.last()
            val categoryName = parts.dropLast(1).joinToString("_")
            val category = try { AchievementCategory.valueOf(categoryName) } catch (_: Exception) { return null }
            val tier = try { AchievementTier.valueOf(tierName) } catch (_: Exception) { return null }
            return UnlockedAchievement(category, tier)
        }
    }
}
