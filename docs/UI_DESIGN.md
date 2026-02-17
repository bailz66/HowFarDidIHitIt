# UI/UX Design — Modern Look and Feel
> GitHub Issue: #5

## Design Philosophy
The app should look and feel like a modern, polished mobile app that belongs on the Play Store alongside industry leaders. Clean, minimal, and instantly usable.

## Design System: Material Design 3

### Why Material 3?
- **Industry standard** for Android — Google's official design system
- **Dynamic Color** — adapts to the user's wallpaper (Android 12+)
- Built into Jetpack Compose via `MaterialTheme`
- Consistent with the rest of the Android ecosystem
- Accessibility built-in (contrast ratios, touch targets, screen readers)

### Color Scheme
```kotlin
// Dynamic color (adapts to wallpaper on Android 12+)
// Fallback: Golf-inspired green palette
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),        // Forest green
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7), // Light green
    secondary = Color(0xFF558B2F),       // Olive green
    surface = Color(0xFFFFFBFE),         // Near white
    background = Color(0xFFFFFBFE),
    error = Color(0xFFBA1A1A),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),         // Soft green
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFA5D6A7),
    surface = Color(0xFF1C1B1F),
    background = Color(0xFF1C1B1F),
    error = Color(0xFFFFB4AB),
)
```

### Typography
```kotlin
val Typography = Typography(
    // Large prominent numbers (distance display)
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp
    ),
    // Screen titles
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    // Club names, stat labels
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    // Body text, descriptions
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Small metadata (timestamps, secondary info)
    labelSmall = TextStyle(
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)
```

## Screen Designs

### Shot Tracker (Home Screen)
```
┌─────────────────────────────────┐
│  ● How Far Did I Hit It         │ ← Top app bar
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐    │
│  │ SELECT CLUB             │    │ ← Club selector
│  │ ┌──────┐┌──────┐┌─────┐│    │   (horizontal scrollable chips)
│  │ │Driver││3 Wood││5 Wd ││    │
│  │ └──────┘└──────┘└─────┘│    │
│  │ ┌──────┐┌──────┐┌─────┐│    │
│  │ │7 Iron││8 Iron││9 Ir ││    │
│  │ └──────┘└──────┘└─────┘│    │
│  └─────────────────────────┘    │
│                                 │
│         ╭──────────────╮        │
│         │              │        │ ← Big, obvious action button
│         │  MARK START  │        │   (FilledTonalButton, large)
│         │              │        │
│         ╰──────────────╯        │
│                                 │
│   📍 GPS Ready • Accuracy: 3m  │ ← Status bar
│                                 │
├────────┬────────────┬───────────┤
│Tracker │ Analytics  │ History   │ ← Bottom navigation
└────────┴────────────┴───────────┘
```

**After Mark Start (Walking State):**
```
┌─────────────────────────────────┐
│  ● How Far Did I Hit It         │
├─────────────────────────────────┤
│                                 │
│  Club: Driver                   │
│  Start pinned ✓                 │
│                                 │
│        ┌─────────────┐          │
│        │    142      │          │ ← Large distance number
│        │   yards     │          │   (animates as you walk)
│        │   (130m)    │          │
│        └─────────────┘          │
│                                 │
│         ╭──────────────╮        │
│         │              │        │
│         │   MARK END   │        │ ← Changes to "Mark End"
│         │              │        │
│         ╰──────────────╯        │
│                                 │
│   [Reset]                       │ ← Secondary action
│                                 │
├────────┬────────────┬───────────┤
│Tracker │ Analytics  │ History   │
└────────┴────────────┴───────────┘
```

**Shot Result:**
```
┌─────────────────────────────────┐
│  ● Shot Complete!               │
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐    │
│  │       DRIVER             │   │ ← Card with shot result
│  │                          │   │
│  │        245               │   │ ← Hero number
│  │       yards              │   │
│  │      (224m)              │   │
│  │                          │   │
│  │  ┌────────────────────┐  │   │
│  │  │ 72°F  Clear sky    │  │   │ ← Weather summary
│  │  │ Wind: 8 mph NW     │  │   │
│  │  └────────────────────┘  │   │
│  │                          │   │
│  │       Saved ✓            │   │
│  └─────────────────────────┘    │
│                                 │
│         ╭──────────────╮        │
│         │  NEXT SHOT   │        │
│         ╰──────────────╯        │
│                                 │
├────────┬────────────┬───────────┤
│Tracker │ Analytics  │ History   │
└────────┴────────────┴───────────┘
```

### Analytics Screen
```
┌─────────────────────────────────┐
│  ● Analytics                    │
├─────────────────────────────────┤
│ ┌──────┐┌───────┐┌────────┐    │ ← Filter chips
│ │Driver││Last 30d││Clear ✕ │    │
│ └──────┘└───────┘└────────┘    │
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐    │
│  │ DRIVER            23 shots│  │ ← Club stat card
│  │ ───────────────────────  │   │   (ElevatedCard)
│  │ Avg: 243 yds             │   │
│  │ Long: 267 yds            │   │
│  │ Short: 218 yds           │   │
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 7 IRON            45 shots│  │
│  │ ───────────────────────  │   │
│  │ Avg: 152 yds             │   │
│  │ Long: 165 yds            │   │
│  │ Short: 138 yds           │   │
│  └─────────────────────────┘    │
│                                 │
│  ... (scrollable)               │
│                                 │
├────────┬────────────┬───────────┤
│Tracker │ Analytics  │ History   │
└────────┴────────────┴───────────┘
```

### Shot History Screen
```
┌─────────────────────────────────┐
│  ● History                      │
├─────────────────────────────────┤
│ ┌──────────┐┌──────┐           │ ← Same filter chips
│ │All Clubs ││All Time│          │
│ └──────────┘└──────┘           │
├─────────────────────────────────┤
│                                 │
│  Feb 17, 2026 — 2:30 PM        │ ← Date header
│  ┌─────────────────────────┐    │
│  │ Driver   245 yds (224m) │    │ ← Shot row
│  │ 72°F Clear sky  8mph NW │    │   (ListItem style)
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 7 Iron   152 yds (139m) │    │
│  │ 72°F Clear sky  8mph NW │    │
│  └─────────────────────────┘    │
│                                 │
│  Feb 16, 2026 — 4:15 PM        │
│  ┌─────────────────────────┐    │
│  │ PW       118 yds (108m) │    │
│  │ 65°F Partly cloudy      │    │
│  └─────────────────────────┘    │
│                                 │
├────────┬────────────┬───────────┤
│Tracker │ Analytics  │ History   │
└────────┴────────────┴───────────┘
```

## Component Library

### Buttons
- **Primary action** (Mark Start, Mark End): `FilledTonalButton`, large, full-width
- **Secondary action** (Reset, Next Shot): `OutlinedButton` or `TextButton`
- **Navigation**: Material 3 `NavigationBar` with 3 items

### Cards
- **Shot result**: `ElevatedCard` with prominent distance number
- **Club stat**: `ElevatedCard` with stats grid
- **Shot history row**: `ListItem` with leading icon, headline, supporting text

### Chips
- **Club selector**: `FilterChip` in a `FlowRow`
- **Active filters**: `InputChip` with trailing close icon

### Indicators
- **Calibrating**: `CircularProgressIndicator` with "Calibrating..." text
- **GPS status**: Small dot indicator (green = ready, yellow = low accuracy, red = unavailable)
- **Empty state**: Centered illustration/icon + text + CTA button

## Animations & Motion
- **Distance counter**: Animate number changes with `animateFloatAsState`
- **State transitions**: `AnimatedContent` between tracker states (idle → calibrating → walking → result)
- **Card entry**: `AnimatedVisibility` with `fadeIn + slideInVertically` for list items
- **Button state**: Subtle scale animation on press (`Modifier.clickable` with `indication`)
- Keep all animations under 300ms — snappy, not sluggish

## Accessibility
- All interactive elements have minimum 48dp touch target
- Content descriptions on icons and non-text elements
- Color contrast ratios meet WCAG AA (4.5:1 for text)
- Support TalkBack screen reader
- Distance announced as "245 yards" not just "245"
- Dynamic type — respect system font size settings

## Dark Mode
- Full dark mode support via Material 3 `darkColorScheme`
- Follows system setting automatically
- No manual toggle needed in v1

## Responsive Layout
- Designed for phones (portrait primary)
- `LazyColumn` for scrollable lists (handles any screen height)
- Padding and sizing use `dp` units — scales across densities
- Test on small (5") and large (6.7") screens
