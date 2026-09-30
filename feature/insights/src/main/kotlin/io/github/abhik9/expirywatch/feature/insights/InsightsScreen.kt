package io.github.abhik9.expirywatch.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.EmptyState
import io.github.abhik9.expirywatch.core.designsystem.theme.ExpiryWatchTheme
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.CategoryWaste
import io.github.abhik9.expirywatch.core.model.Insights
import io.github.abhik9.expirywatch.core.model.InsightsPeriod
import io.github.abhik9.expirywatch.core.model.MonthlyUsage
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun InsightsRoute(viewModel: InsightsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    InsightsScreen(uiState = uiState, period = period, onPeriodChange = viewModel::onPeriodChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InsightsScreen(
    uiState: InsightsUiState,
    period: InsightsPeriod,
    onPeriodChange: (InsightsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_insights_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PeriodSelector(period = period, onPeriodChange = onPeriodChange)
            when (uiState) {
                InsightsUiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                is InsightsUiState.Success -> InsightsContent(uiState.insights)
            }
        }
    }
}

@Composable
private fun PeriodSelector(period: InsightsPeriod, onPeriodChange: (InsightsPeriod) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        InsightsPeriod.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == period,
                onClick = { onPeriodChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = InsightsPeriod.entries.size),
                label = { Text(option.label()) },
            )
        }
    }
}

@Composable
private fun InsightsPeriod.label(): String = stringResource(
    when (this) {
        InsightsPeriod.ONE_MONTH -> R.string.feature_insights_period_month
        InsightsPeriod.THREE_MONTHS -> R.string.feature_insights_period_three_months
        InsightsPeriod.TWELVE_MONTHS -> R.string.feature_insights_period_year
    },
)

@Composable
private fun InsightsContent(insights: Insights) {
    val hasHistory = insights.monthly.any { it.consumed + it.wasted > 0 }
    if (!hasHistory) {
        EmptyState(
            emoji = "📊",
            title = stringResource(R.string.feature_insights_empty_title),
            message = stringResource(R.string.feature_insights_empty_message),
        )
        return
    }
    val colors = ExpiryWatchTheme.chartColors

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(
            label = stringResource(R.string.feature_insights_used_up),
            value = insights.consumedCount.toString(),
            markerColor = colors.consumed,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.feature_insights_thrown_away),
            value = insights.wastedCount.toString(),
            markerColor = colors.wasted,
            modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val percent = NumberFormat.getPercentInstance()
        StatTile(
            label = stringResource(R.string.feature_insights_waste_rate),
            value = insights.wasteRate?.let { percent.format(it) } ?: "–",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.feature_insights_used_in_time),
            value = if (insights.consumedCount == 0) {
                "–"
            } else {
                percent.format(insights.consumedInTimeCount.toDouble() / insights.consumedCount)
            },
            modifier = Modifier.weight(1f),
        )
    }
    Text(
        text = insightsHeadline(insights),
        style = MaterialTheme.typography.bodyLarge,
    )

    InsightsCard(title = stringResource(R.string.feature_insights_trend_title)) {
        MonthlyUsageChart(monthly = insights.monthly)
        var showTable by rememberSaveable { mutableStateOf(false) }
        TextButton(onClick = { showTable = !showTable }) {
            Text(
                stringResource(
                    if (showTable) R.string.feature_insights_hide_table else R.string.feature_insights_show_table,
                ),
            )
        }
        if (showTable) MonthlyTable(insights.monthly)
    }

    InsightsCard(title = stringResource(R.string.feature_insights_categories_title)) {
        if (insights.topWastedCategories.isEmpty()) {
            Text(
                text = stringResource(R.string.feature_insights_nothing_wasted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            CategoryWasteBars(insights.topWastedCategories)
        }
    }
}

@Composable
private fun insightsHeadline(insights: Insights): String {
    val periodText = when (insights.period) {
        InsightsPeriod.ONE_MONTH -> stringResource(R.string.feature_insights_headline_period_month)
        InsightsPeriod.THREE_MONTHS -> stringResource(R.string.feature_insights_headline_period_three_months)
        InsightsPeriod.TWELVE_MONTHS -> stringResource(R.string.feature_insights_headline_period_year)
    }
    return when {
        insights.finishedCount == 0 -> stringResource(R.string.feature_insights_headline_none, periodText)

        insights.wastedCount == 0 -> pluralStringResource(
            R.plurals.feature_insights_headline_no_waste,
            insights.consumedCount,
            insights.consumedCount,
            periodText,
        )

        else -> stringResource(
            R.string.feature_insights_headline,
            pluralStringResource(R.plurals.feature_insights_items, insights.consumedCount, insights.consumedCount),
            pluralStringResource(R.plurals.feature_insights_items, insights.wastedCount, insights.wastedCount),
            periodText,
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    markerColor: Color? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (markerColor != null) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(8.dp)
                            .background(markerColor, CircleShape),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InsightsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

@Composable
private fun MonthlyTable(monthly: List<MonthlyUsage>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TableRow(
            stringResource(R.string.feature_insights_table_month),
            stringResource(R.string.feature_insights_used_up),
            stringResource(R.string.feature_insights_thrown_away),
            header = true,
        )
        monthly.asReversed().forEach { usage ->
            TableRow(usage.month.displayName(), usage.consumed.toString(), usage.wasted.toString())
        }
    }
}

@Composable
private fun TableRow(month: String, consumed: String, wasted: String, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(month, modifier = Modifier.weight(2f), style = style, color = color)
        Text(consumed, modifier = Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End)
        Text(wasted, modifier = Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End)
    }
}

private fun YearMonth.displayName(): String = "${month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} $year"

/** One horizontal bar per category, longest for the most wasted, with the count at its tip. */
@Composable
private fun CategoryWasteBars(entries: List<CategoryWaste>) {
    val barColor = ExpiryWatchTheme.chartColors.wasted
    val maxCount = entries.maxOf { it.wastedCount }.coerceAtLeast(1)
    val uncategorized = stringResource(R.string.feature_insights_uncategorized)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.category?.let { "${it.emoji} ${it.name}" } ?: uncategorized,
                    modifier = Modifier.width(CATEGORY_LABEL_WIDTH),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(entry.wastedCount.toFloat() / maxCount, fill = true)
                            .height(12.dp)
                            .background(barColor, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)),
                    )
                    // Leaves the rest of the track empty for shorter bars.
                    val remainder = 1f - entry.wastedCount.toFloat() / maxCount
                    if (remainder > 0f) Box(Modifier.weight(remainder).fillMaxHeight())
                    Text(
                        text = entry.wastedCount.toString(),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private val CATEGORY_LABEL_WIDTH = 140.dp

@Preview(showBackground = true)
@Composable
private fun InsightsScreenPreview() {
    val dairy = Category(id = 1, name = "Dairy", emoji = "🧀")
    val produce = Category(id = 2, name = "Fruit & vegetables", emoji = "🥕")
    ExpiryWatchTheme(dynamicColor = false) {
        InsightsScreen(
            uiState = InsightsUiState.Success(
                Insights(
                    period = InsightsPeriod.THREE_MONTHS,
                    consumedCount = 42,
                    wastedCount = 6,
                    consumedInTimeCount = 38,
                    monthly = (5 downTo 0).map { monthsAgo ->
                        MonthlyUsage(
                            YearMonth.of(2026, 3).minusMonths(monthsAgo.toLong()),
                            10 + monthsAgo,
                            monthsAgo % 3,
                        )
                    },
                    topWastedCategories = listOf(CategoryWaste(produce, 4), CategoryWaste(dairy, 2)),
                ),
            ),
            period = InsightsPeriod.THREE_MONTHS,
            onPeriodChange = {},
        )
    }
}
