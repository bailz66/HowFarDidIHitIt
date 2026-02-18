package com.example.howfardidihitit.domain

enum class Club(val displayName: String, val category: Category, val sortOrder: Int) {
    DRIVER("Driver", Category.WOOD, 1),
    THREE_WOOD("3 Wood", Category.WOOD, 2),
    FIVE_WOOD("5 Wood", Category.WOOD, 3),
    SEVEN_WOOD("7 Wood", Category.WOOD, 4),
    THREE_IRON("3 Iron", Category.IRON, 5),
    FOUR_IRON("4 Iron", Category.IRON, 6),
    FIVE_IRON("5 Iron", Category.IRON, 7),
    SIX_IRON("6 Iron", Category.IRON, 8),
    SEVEN_IRON("7 Iron", Category.IRON, 9),
    EIGHT_IRON("8 Iron", Category.IRON, 10),
    NINE_IRON("9 Iron", Category.IRON, 11),
    PITCHING_WEDGE("Pitching Wedge", Category.WEDGE, 12),
    GAP_WEDGE("Gap Wedge", Category.WEDGE, 13),
    SAND_WEDGE("Sand Wedge", Category.WEDGE, 14),
    LOB_WEDGE("Lob Wedge", Category.WEDGE, 15),
    PUTTER("Putter", Category.PUTTER, 16),
    HYBRID_3("3 Hybrid", Category.HYBRID, 17),
    HYBRID_4("4 Hybrid", Category.HYBRID, 18);

    enum class Category {
        WOOD, IRON, WEDGE, PUTTER, HYBRID
    }
}
