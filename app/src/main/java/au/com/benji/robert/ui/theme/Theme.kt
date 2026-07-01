package au.com.benji.robert.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import au.com.benji.robert.theme.RobertColors

private val DarkColorScheme = darkColorScheme(
    primary = RobertColors.Primary,
    secondary = RobertColors.Secondary,
    background = RobertColors.Background,
    surface = RobertColors.Surface,
    onPrimary = RobertColors.TextPrimary,
    onSecondary = RobertColors.TextPrimary,
    onBackground = RobertColors.TextPrimary,
    onSurface = RobertColors.TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = RobertColors.PrimaryVariant,
    secondary = RobertColors.Secondary,
    // Add other overrides as needed
)

@Composable
fun RobertTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
