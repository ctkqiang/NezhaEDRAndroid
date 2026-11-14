package xin.ctkqiang.nezhaedr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Red Theme (Default Light)
private val RedLightColorScheme = lightColorScheme(
    primary = RedPrimaryLight,
    secondary = RedSecondaryLight,
    tertiary = RedTertiaryLight,
    primaryContainer = RedContainerLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

// Blue Theme (Light)
private val BlueLightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    secondary = BlueSecondaryLight,
    tertiary = BlueTertiaryLight,
    primaryContainer = BlueContainerLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

// Pink Theme (Light)
private val PinkLightColorScheme = lightColorScheme(
    primary = PinkPrimaryLight,
    secondary = PinkSecondaryLight,
    tertiary = PinkTertiaryLight,
    primaryContainer = PinkContainerLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary
)

@Composable
fun NezhaEDRTheme(
    appTheme: AppThemeMode = AppThemeMode.RED,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppThemeMode.RED -> RedLightColorScheme
        AppThemeMode.BLUE -> BlueLightColorScheme
        AppThemeMode.PINK -> PinkLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
