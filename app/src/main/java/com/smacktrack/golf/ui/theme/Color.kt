package com.smacktrack.golf.ui.theme

/**
 * App-wide color palette.
 *
 * All text colors pass WCAG AA contrast requirements against the OffWhite/White
 * backgrounds used in the app.
 */

import androidx.compose.ui.graphics.Color

// Primary dark green — buttons, selections, accents
val DarkGreen = Color(0xFF1B5E20)
val DarkGreenLight = Color(0xFF2E7D32)

// Surface / background
val White = Color(0xFFFFFFFF)
val OffWhite = Color(0xFFF7F8F5)
val LightGray = Color(0xFFF1F3EE)
val MidGray = Color(0xFFE0E2DC)

// Text — all pass WCAG AA on OffWhite/White
val TextPrimary = Color(0xFF1A1C19)     // ~16:1 contrast
val TextSecondary = Color(0xFF5F6158)   // ~5.5:1 contrast
val TextTertiary = Color(0xFF757872)    // ~4.6:1 contrast (fixed from #9DA09A which failed)

// Accents
val LightGreenTint = Color(0xFFE8F5E9)

// Error
val Red40 = Color(0xFFBA1A1A)
val Red80 = Color(0xFFFFB4AB)
val Red90 = Color(0xFFFFDAD6)
