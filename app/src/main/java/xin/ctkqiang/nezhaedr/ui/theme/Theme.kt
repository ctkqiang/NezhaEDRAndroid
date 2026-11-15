package xin.ctkqiang.nezhaedr.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Red Theme Light/Dark Color Schemes
private val RedLightColorScheme = lightColorScheme(
    primary = RedPrimaryLight,
    secondary = RedSecondaryLight,
    tertiary = RedTertiaryLight,
    primaryContainer = RedContainerLight,
    secondaryContainer = RedContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = RedPrimaryLight,
    onSecondaryContainer = RedPrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val RedDarkColorScheme = darkColorScheme(
    primary = RedTertiaryLight,
    secondary = RedPrimaryLight,
    tertiary = RedSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF450A0A),
    secondaryContainer = ColorContainerDark(0xFF450A0A),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = RedTertiaryLight,
    onSecondaryContainer = RedTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// Helper function for dark container
private fun ColorContainerDark(colorInt: Long) = androidx.compose.ui.graphics.Color(colorInt)

// Blue Theme Light/Dark Color Schemes
private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    secondary = BlueSecondaryLight,
    tertiary = BlueTertiaryLight,
    primaryContainer = BlueContainerLight,
    secondaryContainer = BlueContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = BluePrimaryLight,
    onSecondaryContainer = BluePrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = BlueTertiaryLight,
    secondary = BluePrimaryLight,
    tertiary = BlueSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF075985),
    secondaryContainer = ColorContainerDark(0xFF075985),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = BlueTertiaryLight,
    onSecondaryContainer = BlueTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// Pink Theme Light/Dark Color Schemes
private val PinkLightColorScheme = lightColorScheme(
    primary = PinkPrimaryLight,
    secondary = PinkSecondaryLight,
    tertiary = PinkTertiaryLight,
    primaryContainer = PinkContainerLight,
    secondaryContainer = PinkContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = PinkPrimaryLight,
    onSecondaryContainer = PinkPrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val PinkDarkColorScheme = darkColorScheme(
    primary = PinkTertiaryLight,
    secondary = PinkPrimaryLight,
    tertiary = PinkSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF831843),
    secondaryContainer = ColorContainerDark(0xFF831843),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = PinkTertiaryLight,
    onSecondaryContainer = PinkTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// Green Theme Light/Dark Color Schemes
private val GreenLightColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    secondary = GreenSecondaryLight,
    tertiary = GreenTertiaryLight,
    primaryContainer = GreenContainerLight,
    secondaryContainer = GreenContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = GreenPrimaryLight,
    onSecondaryContainer = GreenPrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = GreenTertiaryLight,
    secondary = GreenPrimaryLight,
    tertiary = GreenSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF064E3B),
    secondaryContainer = ColorContainerDark(0xFF064E3B),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = GreenTertiaryLight,
    onSecondaryContainer = GreenTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// Amber Gold Theme Light/Dark Color Schemes
private val AmberLightColorScheme = lightColorScheme(
    primary = AmberPrimaryLight,
    secondary = AmberSecondaryLight,
    tertiary = AmberTertiaryLight,
    primaryContainer = AmberContainerLight,
    secondaryContainer = AmberContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = AmberPrimaryLight,
    onSecondaryContainer = AmberPrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val AmberDarkColorScheme = darkColorScheme(
    primary = AmberTertiaryLight,
    secondary = AmberPrimaryLight,
    tertiary = AmberSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF78350F),
    secondaryContainer = ColorContainerDark(0xFF78350F),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = AmberTertiaryLight,
    onSecondaryContainer = AmberTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// Purple Theme Light/Dark Color Schemes
private val PurpleLightColorScheme = lightColorScheme(
    primary = PurplePrimaryLight,
    secondary = PurpleSecondaryLight,
    tertiary = PurpleTertiaryLight,
    primaryContainer = PurpleContainerLight,
    secondaryContainer = PurpleContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = PurplePrimaryLight,
    onSecondaryContainer = PurplePrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val PurpleDarkColorScheme = darkColorScheme(
    primary = PurpleTertiaryLight,
    secondary = PurplePrimaryLight,
    tertiary = PurpleSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF581C87),
    secondaryContainer = ColorContainerDark(0xFF581C87),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = PurpleTertiaryLight,
    onSecondaryContainer = PurpleTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// Indigo Theme Light/Dark Color Schemes
private val IndigoLightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    secondary = IndigoSecondaryLight,
    tertiary = IndigoTertiaryLight,
    primaryContainer = IndigoContainerLight,
    secondaryContainer = IndigoContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = IndigoPrimaryLight,
    onSecondaryContainer = IndigoPrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val IndigoDarkColorScheme = darkColorScheme(
    primary = IndigoTertiaryLight,
    secondary = IndigoPrimaryLight,
    tertiary = IndigoSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF312E81),
    secondaryContainer = ColorContainerDark(0xFF312E81),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = IndigoTertiaryLight,
    onSecondaryContainer = IndigoTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// Teal Theme Light/Dark Color Schemes
private val TealLightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    secondary = TealSecondaryLight,
    tertiary = TealTertiaryLight,
    primaryContainer = TealContainerLight,
    secondaryContainer = TealContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = TealPrimaryLight,
    onSecondaryContainer = TealPrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val TealDarkColorScheme = darkColorScheme(
    primary = TealTertiaryLight,
    secondary = TealPrimaryLight,
    tertiary = TealSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF134E4A),
    secondaryContainer = ColorContainerDark(0xFF134E4A),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = TealTertiaryLight,
    onSecondaryContainer = TealTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// Orange Theme Light/Dark Color Schemes
private val OrangeLightColorScheme = lightColorScheme(
    primary = OrangePrimaryLight,
    secondary = OrangeSecondaryLight,
    tertiary = OrangeTertiaryLight,
    primaryContainer = OrangeContainerLight,
    secondaryContainer = OrangeContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = OrangePrimaryLight,
    onSecondaryContainer = OrangePrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = OrangeTertiaryLight,
    secondary = OrangePrimaryLight,
    tertiary = OrangeSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF7C2D12),
    secondaryContainer = ColorContainerDark(0xFF7C2D12),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = OrangeTertiaryLight,
    onSecondaryContainer = OrangeTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

// Slate Theme Light/Dark Color Schemes
private val SlateLightColorScheme = lightColorScheme(
    primary = SlatePrimaryLight,
    secondary = SlateSecondaryLight,
    tertiary = SlateTertiaryLight,
    primaryContainer = SlateContainerLight,
    secondaryContainer = SlateContainerLight,
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = SlatePrimaryLight,
    onSecondaryContainer = SlatePrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

private val SlateDarkColorScheme = darkColorScheme(
    primary = SlateTertiaryLight,
    secondary = SlatePrimaryLight,
    tertiary = SlateSecondaryLight,
    primaryContainer = ColorContainerDark(0xFF0F172A),
    secondaryContainer = ColorContainerDark(0xFF0F172A),
    onPrimary = LightSurface,
    onSecondary = LightSurface,
    onPrimaryContainer = SlateTertiaryLight,
    onSecondaryContainer = SlateTertiaryLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary
)

@Composable
fun NezhaEDRTheme(
    appTheme: AppThemeMode = AppThemeMode.RED,
    darkLightMode: ThemeDarkLightMode = ThemeDarkLightMode.AUTO,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (darkLightMode) {
        ThemeDarkLightMode.AUTO -> systemInDark
        ThemeDarkLightMode.LIGHT -> false
        ThemeDarkLightMode.DARK -> true
    }

    val colorScheme = when (appTheme) {
        AppThemeMode.RED -> if (isDark) RedDarkColorScheme else RedLightColorScheme
        AppThemeMode.BLUE -> if (isDark) BlueDarkColorScheme else BlueLightColorScheme
        AppThemeMode.PINK -> if (isDark) PinkDarkColorScheme else PinkLightColorScheme
        AppThemeMode.GREEN -> if (isDark) GreenDarkColorScheme else GreenLightColorScheme
        AppThemeMode.AMBER -> if (isDark) AmberDarkColorScheme else AmberLightColorScheme
        AppThemeMode.PURPLE -> if (isDark) PurpleDarkColorScheme else PurpleLightColorScheme
        AppThemeMode.INDIGO -> if (isDark) IndigoDarkColorScheme else IndigoLightColorScheme
        AppThemeMode.TEAL -> if (isDark) TealDarkColorScheme else TealLightColorScheme
        AppThemeMode.ORANGE -> if (isDark) OrangeDarkColorScheme else OrangeLightColorScheme
        AppThemeMode.SLATE -> if (isDark) SlateDarkColorScheme else SlateLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let { win ->
                win.statusBarColor = colorScheme.surface.toArgb()
                win.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(win, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
