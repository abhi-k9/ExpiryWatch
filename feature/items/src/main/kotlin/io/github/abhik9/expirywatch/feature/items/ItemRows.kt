package io.github.abhik9.expirywatch.feature.items

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsPaused
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.designsystem.component.EmojiAvatar
import io.github.abhik9.expirywatch.core.designsystem.theme.ExpiryWatchTheme
import io.github.abhik9.expirywatch.core.domain.usecase.ItemGroup
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.ui.ExpiryBadge
import io.github.abhik9.expirywatch.core.ui.ItemThumbnail
import io.github.abhik9.expirywatch.core.ui.colors
import io.github.abhik9.expirywatch.core.ui.formatMedium
import io.github.abhik9.expirywatch.core.ui.formatQuantity
import java.time.LocalDate

/**
 * An item, shown by [content], that can be swiped right to mark it used up, or left to mark it
 * thrown away. The same actions are offered to accessibility services as custom actions. Neither
 * is offered while it's not [enabled], as while items are being selected.
 */
@Composable
internal fun SwipeableItemRow(
    onFinish: (ItemStatus) -> Unit,
    onAddToShoppingList: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Guards against the swipe callback firing more than once for the same gesture.
    var finished by remember { mutableStateOf(false) }
    val finishOnce = { outcome: ItemStatus ->
        if (!finished) {
            finished = true
            onFinish(outcome)
        }
    }
    val dismissState = rememberSwipeToDismissBoxState()
    // Finishes the item once a swipe settles past the threshold; the row then leaves the list.
    LaunchedEffect(dismissState) {
        // A row brought back by undo can get its swiped-away state back from the list: put it in place.
        dismissState.reset()
        snapshotFlow { dismissState.currentValue }.collect { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> finishOnce(ItemStatus.CONSUMED)
                SwipeToDismissBoxValue.EndToStart -> finishOnce(ItemStatus.WASTED)
                SwipeToDismissBoxValue.Settled -> Unit
            }
        }
    }
    val consumedLabel = stringResource(R.string.feature_items_action_consumed)
    val wastedLabel = stringResource(R.string.feature_items_action_wasted)
    val shoppingLabel = stringResource(R.string.feature_items_add_to_shopping_list)

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.semantics {
            if (!enabled) return@semantics
            customActions = listOf(
                CustomAccessibilityAction(consumedLabel) {
                    finishOnce(ItemStatus.CONSUMED)
                    true
                },
                CustomAccessibilityAction(wastedLabel) {
                    finishOnce(ItemStatus.WASTED)
                    true
                },
                CustomAccessibilityAction(shoppingLabel) {
                    onAddToShoppingList()
                    true
                },
            )
        },
        backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
        gesturesEnabled = enabled,
    ) {
        content()
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val statusColors = ExpiryWatchTheme.statusColors
    val (colors, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> statusColors.fresh to Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> statusColors.expired to Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.container)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val consumed = direction == SwipeToDismissBoxValue.StartToEnd
            Icon(
                imageVector = if (consumed) Icons.Outlined.CheckCircle else Icons.Outlined.Delete,
                contentDescription = null,
                tint = colors.onContainer,
            )
            Text(
                text = stringResource(
                    if (consumed) R.string.feature_items_action_consumed else R.string.feature_items_action_wasted,
                ),
                color = colors.onContainer,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * An item. While items are being selected, [selection] says whether it is, and a mark takes the
 * place of its picture. [snoozedUntil] is when its snoozed reminders start again.
 */
@Composable
internal fun ItemRow(
    entry: ItemWithExpiry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selection: ToggleableState? = null,
    snoozedUntil: LocalDate? = null,
) {
    val item = entry.item
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .itemClickable(
                MaterialTheme.shapes.medium,
                onClick,
                onLongClick,
                stringResource(R.string.feature_items_select),
                selection,
            ),
        shape = MaterialTheme.shapes.medium,
        color = selection.rowColor(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selection != null) {
                SelectionMark(selection, Modifier.size(44.dp))
            } else {
                ItemThumbnail(item = item)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = listOfNotNull(
                    item.brand,
                    item.location?.let { "${it.emoji} ${it.name}" },
                    formatQuantity(item.quantity, item.unit),
                ).joinToString(separator = " · ")
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.openedDate != null || snoozedUntil != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.openedDate != null) {
                            Text(
                                text = stringResource(R.string.feature_items_opened),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        snoozedUntil?.let { SnoozedLabel(it) }
                    }
                }
            }
            ExpiryBadge(status = entry.status, daysUntilExpiry = entry.daysUntilExpiry)
        }
    }
}

/**
 * A product with several items, such as cartons of milk that expire on different days, with the
 * one to use next. Tapping it shows or hides the items. While items are being selected,
 * [selection] says whether all of them are, or some. [snoozed] says that all of their reminders
 * are snoozed.
 */
@Composable
internal fun ItemGroupRow(
    group: ItemGroup,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selection: ToggleableState? = null,
    snoozed: Boolean = false,
) {
    val next = group.next
    val items = group.entries.map { it.item }
    val state = stringResource(
        if (expanded) R.string.feature_items_group_expanded else R.string.feature_items_group_collapsed,
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (selection == null) stateDescription = state }
            .itemClickable(
                MaterialTheme.shapes.medium,
                onClick,
                onLongClick,
                stringResource(R.string.feature_items_select),
                selection,
            ),
        shape = MaterialTheme.shapes.medium,
        color = selection.rowColor(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selection != null) {
                SelectionMark(selection, Modifier.size(44.dp))
            } else {
                val customGroup = group.customGroup
                if (customGroup != null) {
                    EmojiAvatar(emoji = customGroup.emoji)
                } else {
                    ItemThumbnail(item = items.firstOrNull { it.imageUrl != null } ?: next.item)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.customGroup?.name ?: next.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = if (group.customGroup != null) {
                    listOf(
                        pluralStringResource(R.plurals.feature_items_group_items, items.size, items.size),
                        stringResource(R.string.feature_items_group_next, next.item.name),
                    )
                } else {
                    listOfNotNull(
                        pluralStringResource(R.plurals.feature_items_group_dates, items.size, items.size),
                        items.totalQuantity(),
                        items.sharedOrNull { it.brand },
                        items.sharedOrNull { it.location }?.let { "${it.emoji} ${it.name}" },
                    )
                }.joinToString(separator = " · ")
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (snoozed) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsPaused,
                    contentDescription = stringResource(R.string.feature_items_reminders_snoozed),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExpiryBadge(status = next.status, daysUntilExpiry = next.daysUntilExpiry)
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One of the items of an [ItemGroupRow], told apart by its printed expiry date. The brand and
 * location are only shown when they differ between the group's items. [selection] and
 * [snoozedUntil] are as for [ItemRow].
 */
@Composable
internal fun GroupEntryRow(
    entry: ItemWithExpiry,
    showBrand: Boolean,
    showLocation: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selection: ToggleableState? = null,
    snoozedUntil: LocalDate? = null,
) {
    val item = entry.item
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .itemClickable(
                MaterialTheme.shapes.medium,
                onClick,
                onLongClick,
                stringResource(R.string.feature_items_select),
                selection,
            ),
        shape = MaterialTheme.shapes.medium,
        color = selection.rowColor(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selection != null) SelectionMark(selection)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.expiryDate.formatMedium(), style = MaterialTheme.typography.titleSmall)
                val details = listOfNotNull(
                    formatQuantity(item.quantity, item.unit),
                    item.brand?.takeIf { showBrand },
                    item.location?.takeIf { showLocation }?.let { "${it.emoji} ${it.name}" },
                    stringResource(R.string.feature_items_opened).takeIf { item.openedDate != null },
                ).joinToString(separator = " · ")
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                snoozedUntil?.let { SnoozedLabel(it) }
            }
            ExpiryBadge(status = entry.status, daysUntilExpiry = entry.daysUntilExpiry)
        }
    }
}

/**
 * Opens the item on a click, and on a long press starts selecting items with it. While items are
 * being selected ([selection] isn't `null`), [onClick] and [onLongClick] select or unselect it.
 */
private fun Modifier.itemClickable(
    shape: Shape,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    selectLabel: String,
    selection: ToggleableState?,
): Modifier =
    this
        .clip(shape)
        .combinedClickable(
            role = if (selection == null) Role.Button else Role.Checkbox,
            onLongClickLabel = selectLabel.takeIf { onLongClick != null && selection == null },
            onLongClick = onLongClick,
            onClick = onClick,
        )
        .semantics { selection?.let { toggleableState = it } }

/** Rows stand out while selected. */
@Composable
private fun ToggleableState?.rowColor(): Color = if (this == ToggleableState.On) {
    MaterialTheme.colorScheme.secondaryContainer
} else {
    MaterialTheme.colorScheme.surfaceContainerLow
}

/** Whether a row is selected: a filled check, a dash when only some of a group's items are, or an empty circle. */
@Composable
private fun SelectionMark(selection: ToggleableState, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = when (selection) {
                ToggleableState.On -> Icons.Filled.CheckCircle
                ToggleableState.Indeterminate -> Icons.Filled.RemoveCircle
                ToggleableState.Off -> Icons.Outlined.Circle
            },
            contentDescription = null,
            tint = if (selection == ToggleableState.Off) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

/** Says that reminders about an item are snoozed, and until when. */
@Composable
private fun SnoozedLabel(until: LocalDate) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.NotificationsPaused,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (until == Item.SNOOZED_INDEFINITELY) {
                stringResource(R.string.feature_items_reminders_snoozed)
            } else {
                stringResource(R.string.feature_items_snoozed_until, until.formatMedium())
            },
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The value every item has, or `null` if they differ. */
private inline fun <T> List<Item>.sharedOrNull(value: (Item) -> T?): T? = map(value).distinct().singleOrNull()

/** How much there is in all, if every item is counted in the same unit. */
@Composable
private fun List<Item>.totalQuantity(): String? =
    sharedOrNull { it.unit }?.let { unit -> formatQuantity(sumOf { it.quantity }, unit) }

@Composable
internal fun ExpiryStatus.headerColor(): Color = colors.accent

@Preview
@Composable
private fun ItemRowPreview() {
    ExpiryWatchTheme(dynamicColor = false) {
        ItemRow(
            entry = ItemWithExpiry(
                item = Item(
                    name = "Greek yogurt",
                    brand = "Fage",
                    category = Category(name = "Dairy", emoji = "🧀"),
                    location = StorageLocation(name = "Fridge", emoji = "🧊"),
                    quantity = 2.0,
                    unit = QuantityUnit.PIECES,
                    expiryDate = LocalDate.of(2026, 3, 11),
                    openedDate = LocalDate.of(2026, 3, 9),
                ),
                status = ExpiryStatus.EXPIRING_SOON,
                daysUntilExpiry = 1,
            ),
            onClick = {},
        )
    }
}
