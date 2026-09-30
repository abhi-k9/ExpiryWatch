package io.github.abhik9.expirywatch.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Series colors for the insights charts. Blue and orange rather than green and red, so the two
 * series stay distinguishable with red-green color blindness. Each mode's pair was checked for
 * lightness, chroma, color-blind separation and contrast against that mode's card surface.
 */
@Immutable
data class ChartColors(
    val consumed: Color,
    val wasted: Color,
    val gridline: Color,
)

internal val LightChartColors = ChartColors(
    consumed = Color(0xFF1F6FB2),
    wasted = Color(0xFFE0632E),
    gridline = Color(0xFFDDE1D6),
)

internal val DarkChartColors = ChartColors(
    consumed = Color(0xFF3987E5),
    wasted = Color(0xFFD95926),
    gridline = Color(0xFF2E332B),
)

internal val LocalChartColors = staticCompositionLocalOf { LightChartColors }
