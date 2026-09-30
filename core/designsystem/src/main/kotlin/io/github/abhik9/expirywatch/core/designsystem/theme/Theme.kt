package io.github.abhik9.expirywatch.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * The app theme.
 *
 * @param dynamicColor use colors derived from the wallpaper on Android 12+. Falls back to the
 * brand colors on older versions.
 */
@Composable
fun ExpiryWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && supportsDynamicTheming() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors

        else -> LightColors
    }
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
        LocalChartColors provides if (darkTheme) DarkChartColors else LightChartColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            shapes = ExpiryWatchShapes,
            content = content,
        )
    }
}

object ExpiryWatchTheme {
    val statusColors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current

    val chartColors: ChartColors
        @Composable
        @ReadOnlyComposable
        get() = LocalChartColors.current
}

fun supportsDynamicTheming(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val ExpiryWatchShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
