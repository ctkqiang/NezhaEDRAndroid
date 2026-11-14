package xin.ctkqiang.nezhaedr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Red Theme (Default)
private val RedLightColorScheme = lightColorScheme(
    primary = RedPrimaryLight,
    secondary = RedSecondaryLight,
    tertiary = RedTertiaryLight,
    primaryContainer = RedContainerLight
)

private val RedDarkColorScheme = darkColorScheme(
    primary = RedPrimaryDark,
    secondary = RedSecondaryDark,
    tertiary = RedTertiaryDark,
    primaryContainer = RedContainerDark
)

// Blue Theme
private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    secondary = BlueSecondaryLight,
    tertiary = BlueTertiaryLight,
    primaryContainer = BlueContainerLight
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = BluePrimaryDark,
    secondary = BlueSecondaryDark,
    tertiary = BlueTertiaryDark,
    primaryContainer = BlueContainerDark
)

// Pink Theme
private val PinkLightColorScheme = lightColorScheme(
    primary = PinkPrimaryLight,
    secondary = PinkSecondaryLight,
    tertiary = PinkTertiaryLight,
    primaryContainer = PinkContainerLight
)

private val PinkDarkColorScheme = darkColorScheme(
    primary = PinkPrimaryDark,
    secondary = PinkSecondaryDark,
    tertiary = PinkTertiaryDark,
    primaryContainer = PinkContainerDark
)

@Composable
fun NezhaEDRTheme(
    appTheme: AppThemeMode = AppThemeMode.RED,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppThemeMode.RED -> if (darkTheme) RedDarkColorScheme else RedLightColorScheme
        AppThemeMode.BLUE -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        AppThemeMode.PINK -> if (darkTheme) PinkDarkColorScheme else PinkLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
