package io.github.abhik9.expirywatch.feature.items

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.designsystem.theme.ExpiryWatchTheme
import io.github.abhik9.expirywatch.core.domain.usecase.ItemGroup
import io.github.abhik9.expirywatch.core.domain.usecase.ItemQuery
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.ui.ExpiryBadge
import io.github.abhik9.expirywatch.core.ui.ItemThumbnail
import io.github.abhik9.expirywatch.core.ui.colors
import io.github.abhik9.expirywatch.core.ui.formatMedium
import io.github.abhik9.expirywatch.core.ui.formatQuantity
import io.github.abhik9.expirywatch.core.ui.label
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemsTopBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    sortOrder: ItemSortOrder,
    onSortOrderChange: (ItemSortOrder) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var searching by rememberSaveable { mutableStateOf(searchText.isNotEmpty()) }
    val closeSearch = {
        searching = false
        onSearchTextChange("")
    }
    BackHandler(enabled = searching, onBack = closeSearch)

    TopAppBar(
        title = {
            if (searching) {
                SearchField(searchText, onSearchTextChange)
            } else {
                Text(stringResource(R.string.feature_items_title))
            }
        },
        navigationIcon = {
            if (searching) {
                IconButton(onClick = closeSearch) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.feature_items_close_search),
                    )
                }
            }
        },
        actions = {
            when {
                !searching -> IconButton(onClick = { searching = true }) {
                    Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.feature_items_search))
                }

                searchText.isNotEmpty() -> IconButton(onClick = { onSearchTextChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_items_clear_search))
                }
            }
            SortMenu(sortOrder, onSortOrderChange)
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun SearchField(searchText: String, onSearchTextChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    TextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(stringResource(R.string.feature_items_search_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun SortMenu(sortOrder: ItemSortOrder, onSortOrderChange: (ItemSortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.feature_items_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ItemSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label()) },
                    leadingIcon = { RadioButton(selected = order == sortOrder, onClick = null) },
                    onClick = {
                        onSortOrderChange(order)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ItemSortOrder.label(): String = stringResource(
    when (this) {
        ItemSortOrder.EXPIRY_SOONEST -> R.string.feature_items_sort_expiry_soonest
        ItemSortOrder.EXPIRY_LATEST -> R.string.feature_items_sort_expiry_latest
        ItemSortOrder.NAME -> R.string.feature_items_sort_name
        ItemSortOrder.RECENTLY_ADDED -> R.string.feature_items_sort_recently_added
    },
)

/** Three tappable tiles counting expired, soon-to-expire and fresh items; tapping filters. */
@Composable
internal fun StatusSummaryRow(
    counts: Map<ExpiryStatus, Int>,
    selectedStatus: ExpiryStatus?,
    onStatusSelected: (ExpiryStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpiryStatus.entries.forEach { status ->
            StatusTile(
                status = status,
                count = counts[status] ?: 0,
                selected = selectedStatus == status,
                onClick = { onStatusSelected(if (selectedStatus == status) null else status) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusTile(
    status: ExpiryStatus,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = status.colors
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (selected) colors.container else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) colors.onContainer else MaterialTheme.colorScheme.onSurface,
        border = if (selected) BorderStroke(2.dp, colors.accent) else null,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Text(
                    text = status.label(),
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun FilterRow(
    query: ItemQuery,
    categories: List<Category>,
    locations: List<StorageLocation>,
    onCategoryFilterChange: (Long?) -> Unit,
    onLocationFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val selectedCategory = categories.firstOrNull { it.id == query.categoryId }
        DropdownFilterChip(
            label = selectedCategory?.let { "${it.emoji} ${it.name}" }
                ?: stringResource(R.string.feature_items_filter_all_categories),
            selected = selectedCategory != null,
            anyLabel = stringResource(R.string.feature_items_filter_all_categories),
            options = categories.map { it.id to "${it.emoji} ${it.name}" },
            onSelect = onCategoryFilterChange,
        )
        val selectedLocation = locations.firstOrNull { it.id == query.locationId }
        DropdownFilterChip(
            label = selectedLocation?.let { "${it.emoji} ${it.name}" }
                ?: stringResource(R.string.feature_items_filter_all_locations),
            selected = selectedLocation != null,
            anyLabel = stringResource(R.string.feature_items_filter_all_locations),
            options = locations.map { it.id to "${it.emoji} ${it.name}" },
            onSelect = onLocationFilterChange,
        )
        if (query.hasFilters) {
            TextButton(onClick = onClearFilters) {
                Text(stringResource(R.string.feature_items_clear_filters))
            }
        }
    }
}

@Composable
private fun DropdownFilterChip(
    label: String,
    selected: Boolean,
    anyLabel: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(anyLabel) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * An item, shown by [content], that can be swiped right to mark it used up, or left to mark it
 * thrown away. The same actions are offered to accessibility services as custom actions.
 */
@Composable
internal fun SwipeableItemRow(
    onFinish: (ItemStatus) -> Unit,
    modifier: Modifier = Modifier,
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
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    finishOnce(ItemStatus.CONSUMED)
                    true
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    finishOnce(ItemStatus.WASTED)
                    true
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )
    val consumedLabel = stringResource(R.string.feature_items_action_consumed)
    val wastedLabel = stringResource(R.string.feature_items_action_wasted)

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction(consumedLabel) {
                    finishOnce(ItemStatus.CONSUMED)
                    true
                },
                CustomAccessibilityAction(wastedLabel) {
                    finishOnce(ItemStatus.WASTED)
                    true
                },
            )
        },
        backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
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

@Composable
internal fun ItemRow(
    entry: ItemWithExpiry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = entry.item
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ItemThumbnail(item = item)
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
                if (item.openedDate != null) {
                    Text(
                        text = stringResource(R.string.feature_items_opened),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            ExpiryBadge(status = entry.status, daysUntilExpiry = entry.daysUntilExpiry)
        }
    }
}

/**
 * A product with several items, such as cartons of milk that expire on different days, with the
 * one to use next. Tapping it shows or hides the items.
 */
@Composable
internal fun ItemGroupRow(
    group: ItemGroup,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = group.next
    val items = group.entries.map { it.item }
    val state = stringResource(
        if (expanded) R.string.feature_items_group_expanded else R.string.feature_items_group_collapsed,
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = state },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ItemThumbnail(item = items.firstOrNull { it.imageUrl != null } ?: next.item)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = next.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = listOfNotNull(
                    pluralStringResource(R.plurals.feature_items_group_dates, items.size, items.size),
                    items.totalQuantity(),
                    items.sharedOrNull { it.brand },
                    items.sharedOrNull { it.location }?.let { "${it.emoji} ${it.name}" },
                ).joinToString(separator = " · ")
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
 * location are only shown when they differ between the group's items.
 */
@Composable
internal fun GroupEntryRow(
    entry: ItemWithExpiry,
    showBrand: Boolean,
    showLocation: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = entry.item
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            }
            ExpiryBadge(status = entry.status, daysUntilExpiry = entry.daysUntilExpiry)
        }
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

@Preview
@Composable
private fun StatusSummaryRowPreview() {
    ExpiryWatchTheme(dynamicColor = false) {
        StatusSummaryRow(
            counts = mapOf(ExpiryStatus.EXPIRED to 1, ExpiryStatus.EXPIRING_SOON to 4, ExpiryStatus.FRESH to 23),
            selectedStatus = ExpiryStatus.EXPIRING_SOON,
            onStatusSelected = {},
        )
    }
}
