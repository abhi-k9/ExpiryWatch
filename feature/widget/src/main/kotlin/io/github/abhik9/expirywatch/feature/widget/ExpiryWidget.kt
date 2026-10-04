package io.github.abhik9.expirywatch.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.common.navigation.DeepLink
import io.github.abhik9.expirywatch.core.designsystem.theme.DarkStatusColors
import io.github.abhik9.expirywatch.core.designsystem.theme.LightStatusColors
import io.github.abhik9.expirywatch.core.designsystem.theme.StatusColor
import io.github.abhik9.expirywatch.core.domain.usecase.ExpirySummary
import io.github.abhik9.expirywatch.core.domain.usecase.GetExpirySummaryUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.ui.ExpiryText

/** A home-screen widget listing the items that expire next. */
class ExpiryWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val getExpirySummary = entryPoint.getExpirySummaryUseCase()
        val initialSummary = getExpirySummary()
        provideContent {
            // Glance runs this once per session and only recomposes on later updates, so the data
            // is observed rather than loaded once.
            val summaries = remember { getExpirySummary.observe() }
            val summary by summaries.collectAsState(initialSummary)
            GlanceTheme {
                ExpiryWidgetContent(summary)
            }
        }
    }
}

class ExpiryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpiryWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun getExpirySummaryUseCase(): GetExpirySummaryUseCase
}

@Composable
private fun ExpiryWidgetContent(summary: ExpirySummary) {
    val context = LocalContext.current
    val needingAttention = summary.expired.size + summary.expiringSoon.size

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(context.deepLinkIntent(DeepLink.Items()))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.feature_widget_title),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = when {
                        summary.items.isEmpty() -> context.getString(R.string.feature_widget_empty)

                        needingAttention == 0 -> context.getString(R.string.feature_widget_all_fresh)

                        else -> context.resources.getQuantityString(
                            R.plurals.feature_widget_needs_attention,
                            needingAttention,
                            needingAttention,
                        )
                    },
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                    maxLines = 1,
                )
            }
            Image(
                provider = ImageProvider(R.drawable.feature_widget_ic_add),
                contentDescription = context.getString(R.string.feature_widget_add_item),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                modifier = GlanceModifier
                    .size(36.dp)
                    .cornerRadius(18.dp)
                    .background(GlanceTheme.colors.primaryContainer)
                    .padding(6.dp)
                    .clickable(actionStartActivity(context.deepLinkIntent(DeepLink.AddItem()))),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(summary.items.take(MAX_ITEMS), itemId = { it.item.id }) { entry ->
                WidgetItemRow(entry)
            }
        }
    }
}

@Composable
private fun WidgetItemRow(entry: ItemWithExpiry) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(actionStartActivity(context.deepLinkIntent(DeepLink.EditItem(entry.item.id)))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .cornerRadius(4.dp)
                .background(entry.status.dotColor()),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = entry.item.name,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
            maxLines = 1,
        )
        Text(
            text = ExpiryText.short(context.resources, entry.daysUntilExpiry),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            maxLines = 1,
        )
    }
}

private fun ExpiryStatus.dotColor(): ColorProvider = when (this) {
    ExpiryStatus.EXPIRED -> dayNight(LightStatusColors.expired, DarkStatusColors.expired)
    ExpiryStatus.EXPIRING_SOON -> dayNight(LightStatusColors.expiringSoon, DarkStatusColors.expiringSoon)
    ExpiryStatus.FRESH -> dayNight(LightStatusColors.fresh, DarkStatusColors.fresh)
}

private fun dayNight(light: StatusColor, dark: StatusColor): ColorProvider =
    DayNightColorProvider(day = light.accent, night = dark.accent)

private fun Context.deepLinkIntent(deepLink: DeepLink): Intent =
    Intent(Intent.ACTION_VIEW, deepLink.toUri().toUri()).setPackage(packageName)

private const val MAX_ITEMS = 8
