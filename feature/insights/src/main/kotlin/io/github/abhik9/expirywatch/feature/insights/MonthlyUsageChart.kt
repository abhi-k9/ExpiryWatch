package io.github.abhik9.expirywatch.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.designsystem.theme.ExpiryWatchTheme
import io.github.abhik9.expirywatch.core.model.MonthlyUsage
import java.time.format.TextStyle
import java.util.Locale

/**
 * Stacked columns of items used up (bottom) and thrown away (top) per month. Tapping a column
 * selects it and shows its exact numbers below, which is also how the values reach screen readers.
 */
@Composable
internal fun MonthlyUsageChart(
    monthly: List<MonthlyUsage>,
    modifier: Modifier = Modifier,
) {
    if (monthly.isEmpty()) return
    val colors = ExpiryWatchTheme.chartColors
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember(monthly.size) { mutableIntStateOf(monthly.lastIndex) }

    val maxTotal = monthly.maxOf { it.consumed + it.wasted }
    val axisMax = niceAxisMax(maxTotal)
    val summary = stringResource(R.string.feature_insights_chart_description, monthly.size)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartLegend()
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
                .semantics { contentDescription = summary }
                .pointerInput(monthly.size) {
                    detectTapGestures { offset ->
                        val slotWidth = (size.width - AXIS_LABEL_WIDTH.toPx()) / monthly.size
                        val index = ((offset.x - AXIS_LABEL_WIDTH.toPx()) / slotWidth).toInt()
                        if (index in monthly.indices) selectedIndex = index
                    }
                },
        ) {
            val axisWidth = AXIS_LABEL_WIDTH.toPx()
            val plotWidth = size.width - axisWidth
            val plotHeight = size.height
            val slotWidth = plotWidth / monthly.size
            val barWidth = minOf(MAX_BAR_WIDTH.toPx(), slotWidth * BAR_FILL)
            val gap = SEGMENT_GAP.toPx()
            val radius = CornerRadius(BAR_RADIUS.toPx())

            // Recessive hairline gridlines at 0, half and the top of the axis, labelled on the left.
            listOf(0, axisMax / 2, axisMax).distinct().forEach { tick ->
                val y = plotHeight - plotHeight * tick / axisMax.coerceAtLeast(1)
                drawLine(colors.gridline, Offset(axisWidth, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val layout = textMeasurer.measure(tick.toString(), labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = axisWidth - layout.size.width - 6.dp.toPx(),
                        y = (y - layout.size.height / 2f).coerceIn(0f, plotHeight - layout.size.height),
                    ),
                )
            }

            monthly.forEachIndexed { index, month ->
                val left = axisWidth + slotWidth * index + (slotWidth - barWidth) / 2
                val consumedHeight = plotHeight * month.consumed / axisMax.coerceAtLeast(1)
                val wastedHeight = plotHeight * month.wasted / axisMax.coerceAtLeast(1)
                val selected = index == selectedIndex
                val alpha = if (selected) 1f else UNSELECTED_ALPHA

                if (selected) {
                    drawRect(
                        color = labelColor.copy(alpha = SELECTION_ALPHA),
                        topLeft = Offset(axisWidth + slotWidth * index, 0f),
                        size = Size(slotWidth, plotHeight),
                    )
                }
                var top = plotHeight
                if (month.consumed > 0) {
                    top -= consumedHeight
                    drawBarSegment(
                        color = colors.consumed.copy(alpha = alpha),
                        left = left,
                        top = top,
                        width = barWidth,
                        height = consumedHeight,
                        roundTop = month.wasted == 0,
                        radius = radius,
                    )
                }
                if (month.wasted > 0) {
                    // A surface-colored gap separates the stacked segments.
                    val segmentTop = top - wastedHeight
                    val segmentHeight = if (month.consumed > 0) wastedHeight - gap else wastedHeight
                    drawBarSegment(
                        color = colors.wasted.copy(alpha = alpha),
                        left = left,
                        top = segmentTop,
                        width = barWidth,
                        height = segmentHeight.coerceAtLeast(1f),
                        roundTop = true,
                        radius = radius,
                    )
                }
            }
        }
        MonthLabels(monthly)
        SelectedMonthDetails(monthly[selectedIndex.coerceIn(monthly.indices)])
    }
}

/** Draws a bar segment that is square at the bottom and, when [roundTop], rounded at the top. */
private fun DrawScope.drawBarSegment(
    color: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    roundTop: Boolean,
    radius: CornerRadius,
) {
    val topRadius = if (roundTop) radius else CornerRadius.Zero
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = left + width,
                bottom = top + height,
                topLeftCornerRadius = topRadius,
                topRightCornerRadius = topRadius,
                bottomRightCornerRadius = CornerRadius.Zero,
                bottomLeftCornerRadius = CornerRadius.Zero,
            ),
        )
    }
    drawPath(path, color)
}

@Composable
private fun ChartLegend() {
    val colors = ExpiryWatchTheme.chartColors
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendEntry(colors.consumed, stringResource(R.string.feature_insights_used_up))
        LegendEntry(colors.wasted, stringResource(R.string.feature_insights_thrown_away))
    }
}

@Composable
internal fun LegendEntry(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonthLabels(monthly: List<MonthlyUsage>) {
    Row(modifier = Modifier.padding(start = AXIS_LABEL_WIDTH)) {
        monthly.forEach { usage ->
            Text(
                text = usage.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SelectedMonthDetails(usage: MonthlyUsage) {
    val monthName = usage.month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
    Text(
        text = stringResource(
            R.string.feature_insights_month_details,
            "$monthName ${usage.month.year}",
            usage.consumed,
            usage.wasted,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** The smallest round axis maximum (1, 2, 5, 10, 20, 50, …) that fits [value]. */
internal fun niceAxisMax(value: Int): Int {
    if (value <= 1) return 1
    var magnitude = 1
    while (true) {
        for (multiplier in intArrayOf(1, 2, 5)) {
            val candidate = magnitude * multiplier
            if (candidate >= value) return candidate
        }
        magnitude *= 10
    }
}

private val CHART_HEIGHT = 160.dp
private val AXIS_LABEL_WIDTH = 28.dp
private val MAX_BAR_WIDTH = 24.dp
private val SEGMENT_GAP = 2.dp
private val BAR_RADIUS = 4.dp
private const val BAR_FILL = 0.6f
private const val UNSELECTED_ALPHA = 0.85f
private const val SELECTION_ALPHA = 0.08f
