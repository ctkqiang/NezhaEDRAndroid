package xin.ctkqiang.nezhaedr.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
    RED,
    BLUE,
    PINK
}

// Deep Obsidian Glass Base Surfaces
val GlassObsidianBackground = Color(0xFF09090B)
val GlassSurface = Color(0xFF121215)
val GlassCardSurface = Color(0xFF18181C)
val GlassCardAlpha = Color.White.copy(alpha = 0.05f)
val GlassSpecularBorder = Color.White.copy(alpha = 0.12f)
val GlassSpecularBorderPressed = Color.White.copy(alpha = 0.25f)

// Sharp Crimson Threat Accent (Default Red Theme)
val RedPrimaryLight = Color(0xFFE53935)
val RedSecondaryLight = Color(0xFFD32F2F)
val RedTertiaryLight = Color(0xFFFF5252)
val RedContainerLight = Color(0xFF2A0808)

val RedPrimaryDark = Color(0xFFFF4D4D)
val RedSecondaryDark = Color(0xFFFF6666)
val RedTertiaryDark = Color(0xFFFF8A80)
val RedContainerDark = Color(0xFF3B0A0A)

// Cyber Blue Accent Theme
val BluePrimaryLight = Color(0xFF0288D1)
val BlueSecondaryLight = Color(0xFF0277BD)
val BlueTertiaryLight = Color(0xFF039BE5)
val BlueContainerLight = Color(0xFF042033)

val BluePrimaryDark = Color(0xFF4FC3F7)
val BlueSecondaryDark = Color(0xFF81D4FA)
val BlueTertiaryDark = Color(0xFF80D8FF)
val BlueContainerDark = Color(0xFF00273F)

// Neon Pink Accent Theme
val PinkPrimaryLight = Color(0xFFD81B60)
val PinkSecondaryLight = Color(0xFFC2185B)
val PinkTertiaryLight = Color(0xFFE91E63)
val PinkContainerLight = Color(0xFF330517)

val PinkPrimaryDark = Color(0xFFF48FB1)
val PinkSecondaryDark = Color(0xFFF8BBD0)
val PinkTertiaryDark = Color(0xFFFF80AB)
val PinkContainerDark = Color(0xFF4A0025)
