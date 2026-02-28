package com.smacktrack.golf.domain

/**
 * Unlockable achievements (badges) that reward milestones and play patterns.
 */
enum class Achievement(val title: String, val description: String, val icon: String) {
    FIRST_BLOOD("First Blood", "Record your first shot", "\uD83C\uDFAF"),
    CLUB_250("Bomber", "Driver over 250 yards", "\uD83D\uDCA3"),
    CENTURY("Century Club", "Track 100 shots", "\uD83D\uDCAF"),
    FULL_BAG("Full Bag", "Use every enabled club at least once", "\uD83C\uDFCC\uFE0F"),
    PB_MACHINE("PB Machine", "Beat your PB 3 times in a month", "\uD83D\uDE80"),
    WIND_WARRIOR("Wind Warrior", "Hit a shot in 30+ km/h wind", "\uD83C\uDF2C\uFE0F"),
    SNIPER("Sniper", "5 same-club shots within 10 yd spread", "\uD83C\uDFAF"),
    HOT_STREAK("Hot Streak", "3 shots in a row above club average", "\uD83D\uDD25"),
    WEATHERPROOF("Weatherproof", "Shots in 3+ different weather conditions", "\u2614"),
    IRON_MAN("Iron Man", "50 shots with irons", "\uD83E\uDDBE"),
    DAWN_PATROL("Dawn Patrol", "Hit a shot before 7 AM", "\uD83C\uDF05"),
    NIGHT_OWL("Night Owl", "Hit a shot after 8 PM", "\uD83C\uDF19");

    companion object {
        val TOTAL = entries.size
    }
}
