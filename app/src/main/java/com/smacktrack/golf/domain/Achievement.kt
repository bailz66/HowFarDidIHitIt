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
            TierDef(50, "Track 50 shots"),
            TierDef(250, "Track 250 shots"),
            TierDef(1000, "Track 1,000 shots"),
            TierDef(2500, "Track 2,500 shots")
        )
    ),
    BOMBER(
        "Bomber", "\uD83D\uDCA3",
        listOf(
            TierDef(200, "Driver over 200 yards"),
            TierDef(250, "Driver over 250 yards"),
            TierDef(300, "Driver over 300 yards"),
            TierDef(325, "Driver over 325 yards"),
            TierDef(350, "Driver over 350 yards")
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
            TierDef(5, "Beat your PB 5 times"),
            TierDef(20, "Beat your PB 20 times"),
            TierDef(50, "Beat your PB 50 times"),
            TierDef(100, "Beat your PB 100 times")
        )
    ),
    WIND_WARRIOR(
        "Wind Warrior", "\uD83C\uDF2C\uFE0F",
        listOf(
            TierDef(15, "Shot in 15+ km/h wind"),
            TierDef(25, "Shot in 25+ km/h wind"),
            TierDef(35, "Shot in 35+ km/h wind"),
            TierDef(45, "Shot in 45+ km/h wind"),
            TierDef(60, "Shot in 60+ km/h wind")
        )
    ),
    SNIPER(
        "Sniper", "\uD83C\uDFF9",
        listOf(
            TierDef(3, "3 same-club shots within 15 yds", secondaryThreshold = 15),
            TierDef(5, "5 same-club shots within 12 yds", secondaryThreshold = 12),
            TierDef(8, "8 same-club shots within 10 yds", secondaryThreshold = 10),
            TierDef(12, "12 same-club shots within 8 yds", secondaryThreshold = 8),
            TierDef(15, "15 same-club shots within 5 yds", secondaryThreshold = 5)
        )
    ),
    HOT_STREAK(
        "Hot Streak", "\uD83D\uDD25",
        listOf(
            TierDef(3, "3 consecutive above club avg"),
            TierDef(5, "5 consecutive above club avg"),
            TierDef(8, "8 consecutive above club avg"),
            TierDef(12, "12 consecutive above club avg"),
            TierDef(20, "20 consecutive above club avg")
        )
    ),
    WEATHERPROOF(
        "Weatherproof", "\u2614",
        listOf(
            TierDef(2, "Play in 2 weather types"),
            TierDef(3, "Play in 3 weather types"),
            TierDef(4, "Play in 4 weather types"),
            TierDef(5, "Play in 5 weather types"),
            TierDef(7, "Play in all 7 weather types")
        )
    ),
    IRON_MAN(
        "Iron Man", "\uD83E\uDDBE",
        listOf(
            TierDef(10, "10 shots with irons"),
            TierDef(50, "50 shots with irons"),
            TierDef(200, "200 shots with irons"),
            TierDef(500, "500 shots with irons"),
            TierDef(1500, "1,500 shots with irons")
        )
    ),
    DAWN_PATROL(
        "Dawn Patrol", "\uD83C\uDF05",
        listOf(
            TierDef(1, "1 shot before 7 AM"),
            TierDef(10, "10 shots before 7 AM"),
            TierDef(30, "30 shots before 7 AM"),
            TierDef(75, "75 shots before 7 AM"),
            TierDef(150, "150 shots before 7 AM")
        )
    ),
    NIGHT_OWL(
        "Night Owl", "\uD83C\uDF19",
        listOf(
            TierDef(1, "1 shot after 8 PM"),
            TierDef(10, "10 shots after 8 PM"),
            TierDef(30, "30 shots after 8 PM"),
            TierDef(75, "75 shots after 8 PM"),
            TierDef(150, "150 shots after 8 PM")
        )
    ),
    DEDICATED(
        "Dedicated", "\uD83D\uDCC5",
        listOf(
            TierDef(5, "5 practice sessions"),
            TierDef(20, "20 practice sessions"),
            TierDef(50, "50 practice sessions"),
            TierDef(100, "100 practice sessions"),
            TierDef(200, "200 practice sessions")
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
