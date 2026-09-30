package io.github.abhik9.expirywatch.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors for the three expiry states. They stay fixed under dynamic color, so "expired"
 * always reads as red and "fresh" as green whatever the wallpaper.
 */
@Immutable
data class StatusColors(
    val expired: StatusColor,
    val expiringSoon: StatusColor,
    val fresh: StatusColor,
)

/**
 * @property accent for small marks such as dots and chart bars on the surface color.
 * @property container a tinted background, with [onContainer] for text on it.
 */
@Immutable
data class StatusColor(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

val LightStatusColors = StatusColors(
    expired = StatusColor(accent = Color(0xFFBA1A1A), container = Color(0xFFFFDAD5), onContainer = Color(0xFF930009)),
    expiringSoon = StatusColor(
        accent = Color(0xFF7F5700),
        container = Color(0xFFFFDEAE),
        onContainer = Color(0xFF604100),
    ),
    fresh = StatusColor(accent = Color(0xFF006E2E), container = Color(0xFFC9EFCF), onContainer = Color(0xFF005321)),
)

val DarkStatusColors = StatusColors(
    expired = StatusColor(accent = Color(0xFFFFB4AB), container = Color(0xFF930009), onContainer = Color(0xFFFFDAD5)),
    expiringSoon = StatusColor(
        accent = Color(0xFFFFBA3E),
        container = Color(0xFF604100),
        onContainer = Color(0xFFFFDEAE),
    ),
    fresh = StatusColor(accent = Color(0xFF70DD85), container = Color(0xFF005321), onContainer = Color(0xFFB4F2BE)),
)

internal val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
