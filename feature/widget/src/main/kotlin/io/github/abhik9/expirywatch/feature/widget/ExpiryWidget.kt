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
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.currentState
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
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
import io.github.abhik9.expirywatch.core.domain.repository.CustomGroupRepository
import io.github.abhik9.expirywatch.core.domain.usecase.ExpirySummary
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.GetExpirySummaryUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.ui.ExpiryText
import kotlinx.coroutines.flow.first

/**
 * A home-screen widget listing the items that expire next, all of them, those needing attention,
 * or a group's, with buttons to mark them used up or thrown away.
 */
class ExpiryWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val getExpirySummary = entryPoint.getExpirySummaryUseCase()
        val groupRepository = entryPoint.customGroupRepository()
        val initialSummary = getExpirySummary()
        val initialGroups = groupRepository.observeGroups().first()
        provideContent {
            // Glance runs this once per session and only recomposes on later updates, so the data
            // is observed rather than loaded once.
            val summaries = remember { getExpirySummary.observe() }
            val summary by summaries.collectAsState(initialSummary)
            val groupFlow = remember { groupRepository.observeGroups() }
            val groups by groupFlow.collectAsState(initialGroups)
            val filter = WidgetFilter.decode(currentState<Preferences>()[FilterKey], groups)
            GlanceTheme {
                ExpiryWidgetContent(summary, filter, groups)
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

    fun customGroupRepository(): CustomGroupRepository

    fun finishItemUseCase(): FinishItemUseCase
}

@Composable
private fun ExpiryWidgetContent(summary: ExpirySummary, filter: WidgetFilter, groups: List<CustomGroup>) {
    val context = LocalContext.current
    val needingAttention = summary.expired.size + summary.expiringSoon.size
    // Wide enough for the buttons on each row, next to the name and the date.
    val showActions = LocalSize.current.width >= ACTIONS_MIN_WIDTH

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
        Spacer(GlanceModifier.height(6.dp))
        FilterPill(filter, groups)
        Spacer(GlanceModifier.height(4.dp))
        val listed = filter.apply(summary)
        if (listed.isEmpty() && summary.items.isNotEmpty()) {
            Text(
                text = context.getString(R.string.feature_widget_nothing_here),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                modifier = GlanceModifier.padding(vertical = 5.dp),
            )
        }
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(listed.take(MAX_ITEMS), itemId = { it.item.id }) { entry ->
                WidgetItemRow(entry, showActions)
            }
        }
    }
}

/** Shows which items are listed; tapping it shows the next [WidgetFilter]. */
@Composable
private fun FilterPill(filter: WidgetFilter, groups: List<CustomGroup>) {
    val context = LocalContext.current
    val label = when (filter) {
        WidgetFilter.All -> context.getString(R.string.feature_widget_filter_all)
        WidgetFilter.NeedsAttention -> context.getString(R.string.feature_widget_filter_attention)
        is WidgetFilter.Group -> groups.first { it.id == filter.groupId }.let { "${it.emoji} ${it.name}" }
    }
    Text(
        text = context.getString(R.string.feature_widget_filter, label),
        style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 12.sp),
        maxLines = 1,
        modifier = GlanceModifier
            .cornerRadius(12.dp)
            .background(GlanceTheme.colors.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clickable(
                actionRunCallback<ChangeFilterAction>(
                    actionParametersOf(ChangeFilterAction.NextFilter to filter.next(groups).encode()),
                ),
            ),
    )
}

@Composable
private fun WidgetItemRow(entry: ItemWithExpiry, showActions: Boolean) {
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
            // The group's emoji tells items kept together apart from the rest.
            text = entry.item.customGroup?.let { "${it.emoji} ${entry.item.name}" } ?: entry.item.name,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
            maxLines = 1,
        )
        Text(
            text = ExpiryText.short(context.resources, entry.daysUntilExpiry),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            maxLines = 1,
        )
        if (showActions) {
            Spacer(GlanceModifier.width(4.dp))
            FinishButton(
                entry,
                ItemStatus.CONSUMED,
                R.drawable.feature_widget_ic_check,
                R.string.feature_widget_used_up,
            )
            FinishButton(
                entry,
                ItemStatus.WASTED,
                R.drawable.feature_widget_ic_delete,
                R.string.feature_widget_thrown_away,
            )
        }
    }
}

@Composable
private fun FinishButton(entry: ItemWithExpiry, outcome: ItemStatus, iconRes: Int, labelRes: Int) {
    val context = LocalContext.current
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = context.getString(labelRes, entry.item.name),
        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
        modifier = GlanceModifier
            .size(30.dp)
            .cornerRadius(15.dp)
            .padding(5.dp)
            .clickable(
                actionRunCallback<FinishItemAction>(
                    actionParametersOf(
                        FinishItemAction.ItemId to entry.item.id,
                        FinishItemAction.Outcome to outcome.name,
                    ),
                ),
            ),
    )
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
private val ACTIONS_MIN_WIDTH = 220.dp
