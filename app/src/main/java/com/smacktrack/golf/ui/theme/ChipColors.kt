package com.smacktrack.golf.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.smacktrack.golf.location.WindCalculator

// ── Club chip gradient ──────────────────────────────────────────────────────
// Driver (sortOrder 1) gets the darkest green; LW (sortOrder 18) gets the lightest.
val ChipGreenDark = Color(0xFF1B5E20)
val ChipGreenLight = Color(0xFF43A047)

// Unselected / neutral chip colors
val ChipUnselectedBg = Color(0xFFF2F3EF)
val ChipUnselectedText = Color(0xFF3A3D36)
val ChipBorder = Color(0xFFD5D8D0)

/**
 * Returns a green shade for a club chip based on its [sortOrder] (1-18).
 * Lower sort order = darker green (woods), higher = lighter green (wedges).
 */
fun clubChipColor(sortOrder: Int): Color {
    val fraction = (sortOrder - 1) / 17f
    return lerp(ChipGreenDark, ChipGreenLight, fraction)
}

/**
 * Maps a [WindCalculator.WindColorCategory] to a display color.
 * Green shades = helping (tailwind), red shades = hurting (headwind),
 * orange = crosswind.
 */
fun windCategoryColor(cat: WindCalculator.WindColorCategory): Color = when (cat) {
    WindCalculator.WindColorCategory.STRONG_HELPING  -> Color(0xFF2E7D32)
    WindCalculator.WindColorCategory.HELPING         -> Color(0xFF558B2F)
    WindCalculator.WindColorCategory.SLIGHT_HELPING  -> Color(0xFF9E9D24)
    WindCalculator.WindColorCategory.CROSSWIND       -> Color(0xFFE65100)
    WindCalculator.WindColorCategory.SLIGHT_HURTING  -> Color(0xFFD84315)
    WindCalculator.WindColorCategory.HURTING         -> Color(0xFFC62828)
    WindCalculator.WindColorCategory.STRONG_HURTING  -> Color(0xFFB71C1C)
}
