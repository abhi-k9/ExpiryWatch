package io.github.abhik9.expirywatch.feature.items

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.NotificationsPaused
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.designsystem.component.EmojiAvatar
import io.github.abhik9.expirywatch.core.designsystem.theme.ExpiryWatchTheme
import io.github.abhik9.expirywatch.core.domain.repository.ReminderProblem
import io.github.abhik9.expirywatch.core.domain.usecase.ItemGroup
import io.github.abhik9.expirywatch.core.domain.usecase.ItemQuery
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeLength
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.CustomGroup
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

/** Stands in for the top bar while items are selected, with what can be done to all of them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onSnooze: () -> Unit,
    onAddToShoppingList: () -> Unit,
    onMoveToGroup: () -> Unit,
    onSelectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    BackHandler(onBack = onClose)
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.feature_items_selected, count, count)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_items_clear_selection))
            }
        },
        actions = {
            IconButton(onClick = onSnooze) {
                Icon(
                    Icons.Outlined.NotificationsPaused,
                    contentDescription = stringResource(R.string.feature_items_snooze_reminders),
                )
            }
            IconButton(onClick = onAddToShoppingList) {
                Icon(
                    Icons.Outlined.AddShoppingCart,
                    contentDescription = stringResource(R.string.feature_items_add_to_shopping_list),
                )
            }
            IconButton(onClick = onMoveToGroup) {
                Icon(
                    Icons.Outlined.CreateNewFolder,
                    contentDescription = stringResource(R.string.feature_items_move_to_group),
                )
            }
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Outlined.SelectAll, contentDescription = stringResource(R.string.feature_items_select_all))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}

/**
 * How long to leave [items] out of the daily reminder: a week, a month, until the user turns
 * them back on, or until a date they pick. Their reminders can be turned back on when some are
 * snoozed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SnoozeSheet(
    items: List<Item>,
    today: LocalDate,
    onSnooze: (SnoozeLength) -> Unit,
    onPickDate: () -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = items.singleOrNull()
                    ?.let { stringResource(R.string.feature_items_snooze_title_one, it.name) }
                    ?: pluralStringResource(R.plurals.feature_items_snooze_title_many, items.size, items.size),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.feature_items_snooze_help),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SnoozeOption(
                icon = Icons.Outlined.Snooze,
                text = stringResource(R.string.feature_items_snooze_week),
                supportingText = stringResource(
                    R.string.feature_items_snooze_back_on,
                    SnoozeLength.ONE_WEEK.resumeDate(today).formatMedium(),
                ),
                onClick = { onSnooze(SnoozeLength.ONE_WEEK) },
            )
            SnoozeOption(
                icon = Icons.Outlined.Snooze,
                text = stringResource(R.string.feature_items_snooze_month),
                supportingText = stringResource(
                    R.string.feature_items_snooze_back_on,
                    SnoozeLength.ONE_MONTH.resumeDate(today).formatMedium(),
                ),
                onClick = { onSnooze(SnoozeLength.ONE_MONTH) },
            )
            SnoozeOption(
                icon = Icons.Outlined.NotificationsOff,
                text = stringResource(R.string.feature_items_snooze_indefinitely),
                onClick = { onSnooze(SnoozeLength.INDEFINITELY) },
            )
            SnoozeOption(
                icon = Icons.Outlined.Event,
                text = stringResource(R.string.feature_items_snooze_until_date),
                onClick = onPickDate,
            )
            if (items.any { it.isReminderSnoozed(today) }) {
                SnoozeOption(
                    icon = Icons.Outlined.NotificationsActive,
                    text = stringResource(R.string.feature_items_resume_reminders),
                    onClick = onResume,
                )
            }
        }
    }
}

/**
 * Which of the user's [groups] to move the selected items to: one of them, a new one, or no group
 * when [canUngroup] because some of the items are in one.
 */
@Composable
internal fun MoveToGroupDialog(
    groups: List<CustomGroup>,
    canUngroup: Boolean,
    onPick: (CustomGroup?) -> Unit,
    onNewGroup: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_items_move_to_group)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(groups, key = { it.id }) { group ->
                    ListItem(
                        headlineContent = { Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.clickable { onPick(group) },
                        leadingContent = { EmojiAvatar(emoji = group.emoji, size = 36.dp) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
                item(key = "new") {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.feature_items_new_group_option)) },
                        modifier = Modifier.clickable(onClick = onNewGroup),
                        leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
                if (canUngroup) {
                    item(key = "none") {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.feature_items_remove_from_group)) },
                            modifier = Modifier.clickable { onPick(null) },
                            leadingContent = { Icon(Icons.Filled.Close, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_items_cancel)) }
        },
    )
}

/** Warns that reminders may not arrive, naming the most serious of the [problems]. */
@Composable
internal fun ReminderProblemsBanner(
    problems: List<ReminderProblem>,
    onFix: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    stringResource(R.string.feature_items_reminder_problem_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(problems.first().messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_items_reminder_problem_dismiss)) }
            TextButton(onClick = onFix) { Text(stringResource(R.string.feature_items_reminder_problem_fix)) }
        }
    }
}

private fun ReminderProblem.messageRes(): Int = when (this) {
    ReminderProblem.NOTIFICATIONS_BLOCKED -> R.string.feature_items_reminder_problem_notifications
    ReminderProblem.EXACT_ALARMS_BLOCKED -> R.string.feature_items_reminder_problem_alarms
    ReminderProblem.BACKGROUND_RESTRICTED -> R.string.feature_items_reminder_problem_background
    ReminderProblem.LATE -> R.string.feature_items_reminder_problem_late
}

@Composable
private fun SnoozeOption(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    supportingText: String? = null,
) {
    ListItem(
        headlineContent = { Text(text) },
        modifier = Modifier.clickable(onClick = onClick),
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
    groups: List<CustomGroup>,
    onCategoryFilterChange: (Long?) -> Unit,
    onLocationFilterChange: (Long?) -> Unit,
    onGroupFilterChange: (Long?) -> Unit,
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
        // Only once the user has groups, or one is still picked.
        val selectedGroup = groups.firstOrNull { it.id == query.groupId }
        if (groups.isNotEmpty() || query.groupId != null) {
            DropdownFilterChip(
                label = selectedGroup?.let { "${it.emoji} ${it.name}" }
                    ?: stringResource(R.string.feature_items_filter_all_groups),
                selected = query.groupId != null,
                anyLabel = stringResource(R.string.feature_items_filter_all_groups),
                options = groups.sortedBy { it.name.lowercase() }.map { it.id to "${it.emoji} ${it.name}" },
                onSelect = onGroupFilterChange,
            )
        }
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
