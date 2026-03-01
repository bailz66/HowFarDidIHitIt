package com.smacktrack.golf.domain

/**
 * All 18 golf clubs available for shot tracking.
 *
 * Each club has a [displayName] for the UI, a [category] grouping (Wood, Hybrid, Iron, Wedge),
 * and a [sortOrder] (1 = Driver through 18 = LW) used for chip gradient coloring and display order.
 */
enum class Club(val displayName: String, val category: Category, val sortOrder: Int) {
    DRIVER("Driver", Category.WOOD, 1),
    THREE_WOOD("3 Wood", Category.WOOD, 2),
    FIVE_WOOD("5 Wood", Category.WOOD, 3),
    SEVEN_WOOD("7 Wood", Category.WOOD, 4),
    NINE_WOOD("9 Wood", Category.WOOD, 5),
    HYBRID_3("3 Hybrid", Category.HYBRID, 6),
    HYBRID_4("4 Hybrid", Category.HYBRID, 7),
    THREE_IRON("3 Iron", Category.IRON, 8),
    FOUR_IRON("4 Iron", Category.IRON, 9),
    FIVE_IRON("5 Iron", Category.IRON, 10),
    SIX_IRON("6 Iron", Category.IRON, 11),
    SEVEN_IRON("7 Iron", Category.IRON, 12),
    EIGHT_IRON("8 Iron", Category.IRON, 13),
    NINE_IRON("9 Iron", Category.IRON, 14),
    PITCHING_WEDGE("PW", Category.WEDGE, 15),
    GAP_WEDGE("GW", Category.WEDGE, 16),
    SAND_WEDGE("SW", Category.WEDGE, 17),
    LOB_WEDGE("LW", Category.WEDGE, 18);

    enum class Category {
        WOOD, HYBRID, IRON, WEDGE
    }
}
