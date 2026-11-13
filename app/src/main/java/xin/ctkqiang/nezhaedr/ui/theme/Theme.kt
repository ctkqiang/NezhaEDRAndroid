package xin.ctkqiang.nezhaedr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RedDarkColorScheme = darkColorScheme(
    primary = RedPrimaryDark,
    secondary = RedSecondaryDark,
    tertiary = RedTertiaryDark,
    background = GlassObsidianBackground,
    surface = GlassObsidianBackground,
    surfaceVariant = GlassCardSurface,
    primaryContainer = RedContainerDark,
    outline = GlassSpecularBorder
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    secondary = BlueSecondaryDark,
    tertiary = BlueTertiaryDark,
    background = GlassObsidianBackground,
    surface = GlassObsidianBackground,
    surfaceVariant = GlassCardSurface,
    primaryContainer = BlueContainerDark,
    outline = GlassSpecularBorder
)

private val PinkDarkColorScheme = darkColorScheme(
    primary = PinkPrimaryDark,
    secondary = PinkSecondaryDark,
    tertiary = PinkTertiaryDark,
    background = GlassObsidianBackground,
    surface = GlassObsidianBackground,
    surfaceVariant = GlassCardSurface,
    primaryContainer = PinkContainerDark,
    outline = GlassSpecularBorder
)

@Composable
fun NezhaEDRTheme(
    appTheme: AppThemeMode = AppThemeMode.RED,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppThemeMode.RED -> RedDarkColorScheme
        AppThemeMode.BLUE -> BlueDarkColorScheme
        AppThemeMode.PINK -> PinkDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
