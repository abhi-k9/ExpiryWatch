package io.github.abhik9.expirywatch.feature.items

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.EmptyState
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.domain.repository.ReminderProblem
import io.github.abhik9.expirywatch.core.domain.usecase.ItemGroup
import io.github.abhik9.expirywatch.core.domain.usecase.ItemQuery
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeLength
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.ui.LabelDialog
import io.github.abhik9.expirywatch.core.ui.LabelEmojis
import io.github.abhik9.expirywatch.core.ui.PickDateDialog
import io.github.abhik9.expirywatch.core.ui.label
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun ItemsRoute(
    onItemClick: (Item) -> Unit,
    onAddItem: () -> Unit,
    onScanItem: () -> Unit,
    onOpenShoppingList: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ItemsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val reminderProblems by viewModel.reminderProblems.collectAsStateWithLifecycle()
    // The user may change what the system allows while away, e.g. put the app to sleep.
    LifecycleResumeEffect(viewModel) {
        viewModel.recheckReminders()
        onPauseOrDispose {}
    }
    val snackbarHostState = remember { SnackbarHostState() }
    // Updated on configuration changes without restarting the effect, which would drop a shown snackbar.
    val resources by rememberUpdatedState(LocalResources.current)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ItemsEvent.ItemFinished -> {
                    val messageRes = when (event.outcome) {
                        ItemStatus.WASTED -> R.string.feature_items_marked_wasted
                        else -> R.string.feature_items_marked_consumed
                    }
                    val message = resources.getString(messageRes, event.item.name)
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = resources.getString(R.string.feature_items_undo),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoFinish(event.item)
                }

                is ItemsEvent.PutOnShoppingList -> {
                    val names = event.added.names
                    val listName = event.added.list.name
                    val message = names.singleOrNull()
                        ?.let { resources.getString(R.string.feature_items_added_to_list, it, listName) }
                        ?: resources.getQuantityString(
                            R.plurals.feature_items_added_many_to_list,
                            names.size,
                            names.size,
                            listName,
                        )
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = resources.getString(R.string.feature_items_show_list),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) onOpenShoppingList()
                }

                is ItemsEvent.RemindersSnoozed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.snoozedMessage(event.before, event.until),
                        actionLabel = resources.getString(R.string.feature_items_undo),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoSnooze(event.before)
                }

                is ItemsEvent.MovedToGroup -> {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.movedMessage(event.before, event.group),
                        actionLabel = resources.getString(R.string.feature_items_undo),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoMove(event.before)
                }
            }
        }
    }

    ItemsScreen(
        uiState = uiState,
        searchText = viewModel.searchText,
        expandedGroups = viewModel.expandedGroups,
        snackbarHostState = snackbarHostState,
        onSearchTextChange = viewModel::onSearchTextChange,
        onStatusFilterChange = viewModel::onStatusFilterChange,
        onCategoryFilterChange = viewModel::onCategoryFilterChange,
        onLocationFilterChange = viewModel::onLocationFilterChange,
        onClearFilters = viewModel::clearFilters,
        onSortOrderChange = viewModel::onSortOrderChange,
        onGroupClick = viewModel::onGroupClick,
        onItemClick = onItemClick,
        onFinish = viewModel::finish,
        onAddToShoppingList = viewModel::addToShoppingList,
        onAddItem = onAddItem,
        onScanItem = onScanItem,
        onToggleSelection = viewModel::toggleSelection,
        onSelectAll = viewModel::selectAll,
        onClearSelection = viewModel::clearSelection,
        onSnooze = viewModel::snoozeSelected,
        onSnoozeUntil = viewModel::snoozeSelectedUntil,
        onResumeReminders = viewModel::resumeSelectedReminders,
        onAddSelectedToShoppingList = viewModel::addSelectedToShoppingList,
        groups = groups,
        onMoveToGroup = viewModel::moveSelectedToGroup,
        onMoveToNewGroup = viewModel::moveSelectedToNewGroup,
        reminderProblems = reminderProblems,
        onFixReminders = onOpenSettings,
        onDismissReminderProblems = viewModel::dismissReminderProblems,
    )
}

/** E.g. "Moved Beans to Camping kit", or "Took 3 items out of their groups". */
private fun Resources.movedMessage(items: List<Item>, group: CustomGroup?): String {
    val name = items.singleOrNull()?.name
    val count = items.size
    return when {
        group != null && name != null -> getString(R.string.feature_items_moved_one, name, group.name)
        group != null -> getQuantityString(R.plurals.feature_items_moved_many, count, count, group.name)
        name != null -> getString(R.string.feature_items_ungrouped_one, name)
        else -> getQuantityString(R.plurals.feature_items_ungrouped_many, count, count)
    }
}

/** E.g. "Reminders for Milk snoozed until 25 Oct 2026", or "Reminders for 3 items are back on". */
private fun Resources.snoozedMessage(items: List<Item>, until: LocalDate?): String {
    val name = items.singleOrNull()?.name
    val count = items.size
    return when (until) {
        null -> if (name != null) {
            getString(R.string.feature_items_resumed_one, name)
        } else {
            getQuantityString(R.plurals.feature_items_resumed_many, count, count)
        }

        Item.SNOOZED_INDEFINITELY -> if (name != null) {
            getString(R.string.feature_items_snoozed_one_indefinitely, name)
        } else {
            getQuantityString(R.plurals.feature_items_snoozed_many_indefinitely, count, count)
        }

        else -> {
            val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(configuration.locales[0])
                .format(until)
            if (name != null) {
                getString(R.string.feature_items_snoozed_one, name, date)
            } else {
                getQuantityString(R.plurals.feature_items_snoozed_many, count, count, date)
            }
        }
    }
}

/** What's open for snoozing the selected items' reminders. */
private enum class SnoozeStep { OPTIONS, DATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemsScreen(
    uiState: ItemsUiState,
    searchText: String,
    expandedGroups: Set<String>,
    snackbarHostState: SnackbarHostState,
    onSearchTextChange: (String) -> Unit,
    onStatusFilterChange: (ExpiryStatus?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onLocationFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onSortOrderChange: (ItemSortOrder) -> Unit,
    onGroupClick: (String) -> Unit,
    onItemClick: (Item) -> Unit,
    onFinish: (Item, ItemStatus) -> Unit,
    onAddToShoppingList: (List<Item>) -> Unit,
    onAddItem: () -> Unit,
    onScanItem: () -> Unit,
    onToggleSelection: (List<Item>) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSnooze: (SnoozeLength) -> Unit,
    onSnoozeUntil: (LocalDate) -> Unit,
    onResumeReminders: () -> Unit,
    onAddSelectedToShoppingList: () -> Unit,
    groups: List<CustomGroup>,
    onMoveToGroup: (CustomGroup?) -> Unit,
    onMoveToNewGroup: (name: String, emoji: String) -> Unit,
    reminderProblems: List<ReminderProblem>,
    onFixReminders: () -> Unit,
    onDismissReminderProblems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val success = uiState as? ItemsUiState.Success
    val sortOrder = success?.overview?.sortOrder ?: ItemSortOrder.EXPIRY_SOONEST
    val selection = success?.selection.orEmpty()
    val selecting = selection.isNotEmpty()
    var snoozeStep by rememberSaveable { mutableStateOf<SnoozeStep?>(null) }
    var groupStep by rememberSaveable { mutableStateOf<GroupStep?>(null) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (selecting) {
                SelectionTopBar(
                    count = selection.size,
                    onClose = onClearSelection,
                    onSnooze = { snoozeStep = SnoozeStep.OPTIONS },
                    onAddToShoppingList = onAddSelectedToShoppingList,
                    onMoveToGroup = { groupStep = GroupStep.PICK },
                    onSelectAll = onSelectAll,
                    scrollBehavior = scrollBehavior,
                )
            } else {
                ItemsTopBar(
                    searchText = searchText,
                    onSearchTextChange = onSearchTextChange,
                    sortOrder = sortOrder,
                    onSortOrderChange = onSortOrderChange,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        floatingActionButton = {
            if (!selecting) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SmallFloatingActionButton(onClick = onScanItem) {
                        Icon(
                            Icons.Outlined.QrCodeScanner,
                            contentDescription = stringResource(R.string.feature_items_scan),
                        )
                    }
                    ExtendedFloatingActionButton(
                        onClick = onAddItem,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.feature_items_add_item)) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            ItemsUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is ItemsUiState.Success -> ItemsContent(
                state = uiState,
                expandedGroups = expandedGroups,
                contentPadding = innerPadding,
                onStatusFilterChange = onStatusFilterChange,
                onCategoryFilterChange = onCategoryFilterChange,
                onLocationFilterChange = onLocationFilterChange,
                onClearFilters = onClearFilters,
                onGroupClick = onGroupClick,
                onItemClick = onItemClick,
                onFinish = onFinish,
                onAddToShoppingList = onAddToShoppingList,
                onAddItem = onAddItem,
                onToggleSelection = onToggleSelection,
                reminderProblems = reminderProblems,
                onFixReminders = onFixReminders,
                onDismissReminderProblems = onDismissReminderProblems,
            )
        }
    }

    if (success == null || !selecting) return
    val today = success.overview.today
    val selected = success.overview.items.map { it.item }.filter { it.id in selection }
    val close = { snoozeStep = null }
    when (snoozeStep) {
        SnoozeStep.OPTIONS -> SnoozeSheet(
            items = selected,
            today = today,
            onSnooze = { length ->
                close()
                onSnooze(length)
            },
            onPickDate = { snoozeStep = SnoozeStep.DATE },
            onResume = {
                close()
                onResumeReminders()
            },
            onDismiss = close,
        )

        SnoozeStep.DATE -> PickDateDialog(
            initialDate = SnoozeLength.ONE_WEEK.resumeDate(today),
            earliestSelectableDate = today.plusDays(1),
            onConfirm = { date ->
                close()
                onSnoozeUntil(date)
            },
            onDismiss = close,
        )

        null -> Unit
    }

    val closeGroups = { groupStep = null }
    when (groupStep) {
        GroupStep.PICK -> MoveToGroupDialog(
            groups = groups,
            canUngroup = selected.any { it.customGroup != null },
            onPick = { group ->
                closeGroups()
                onMoveToGroup(group)
            },
            onNewGroup = { groupStep = GroupStep.NEW },
            onDismiss = closeGroups,
        )

        GroupStep.NEW -> LabelDialog(
            title = stringResource(R.string.feature_items_new_group),
            confirmText = stringResource(R.string.feature_items_create_and_move),
            suggestions = LabelEmojis.GROUPS,
            onConfirm = { name, emoji ->
                closeGroups()
                onMoveToNewGroup(name, emoji)
            },
            onDismiss = closeGroups,
        )

        null -> Unit
    }
}

/** What's open for moving the selected items to a group. */
private enum class GroupStep { PICK, NEW }

@Composable
private fun ItemsContent(
    state: ItemsUiState.Success,
    expandedGroups: Set<String>,
    contentPadding: PaddingValues,
    onStatusFilterChange: (ExpiryStatus?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onLocationFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onGroupClick: (String) -> Unit,
    onItemClick: (Item) -> Unit,
    onFinish: (Item, ItemStatus) -> Unit,
    onAddToShoppingList: (List<Item>) -> Unit,
    onAddItem: () -> Unit,
    onToggleSelection: (List<Item>) -> Unit,
    reminderProblems: List<ReminderProblem>,
    onFixReminders: () -> Unit,
    onDismissReminderProblems: () -> Unit,
) {
    val overview = state.overview
    if (overview.totalActiveCount == 0 && !state.query.hasFilters) {
        EmptyState(
            emoji = "🧺",
            title = stringResource(R.string.feature_items_empty_title),
            message = stringResource(R.string.feature_items_empty_message),
            modifier = Modifier.padding(contentPadding),
            action = {
                Button(onClick = onAddItem) { Text(stringResource(R.string.feature_items_add_first_item)) }
            },
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            // Leave room so the last item can scroll above the floating action buttons.
            bottom = contentPadding.calculateBottomPadding() + FAB_CLEARANCE,
        ),
    ) {
        if (reminderProblems.isNotEmpty()) {
            item(key = "reminder-problems", contentType = "reminder-problems") {
                ReminderProblemsBanner(
                    problems = reminderProblems,
                    onFix = onFixReminders,
                    onDismiss = onDismissReminderProblems,
                    modifier = Modifier
                        .animateItem()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                )
            }
        }
        item(key = "summary", contentType = "summary") {
            StatusSummaryRow(
                counts = overview.statusCounts,
                selectedStatus = state.query.status,
                onStatusSelected = onStatusFilterChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "filters", contentType = "filters") {
            FilterRow(
                query = state.query,
                categories = state.categories,
                locations = state.locations,
                onCategoryFilterChange = onCategoryFilterChange,
                onLocationFilterChange = onLocationFilterChange,
                onClearFilters = onClearFilters,
            )
        }

        if (overview.items.isEmpty()) {
            item(key = "no-results", contentType = "empty") {
                NoMatchingItems(query = state.query, onClearFilters = onClearFilters)
            }
        } else {
            val expired = overview.items.filter { it.status == ExpiryStatus.EXPIRED }.map { it.item }
            // Filtered to what's expired, the list has no header to offer adding it all on.
            if (state.query.status == ExpiryStatus.EXPIRED && expired.isNotEmpty()) {
                item(key = "add-expired", contentType = "add-expired") {
                    AddExpiredToListButton(
                        onClick = { onAddToShoppingList(expired) },
                        modifier = Modifier
                            .animateItem()
                            .padding(start = 8.dp),
                    )
                }
            }
            overview.sections.forEach { section ->
                section.status?.let { status ->
                    item(key = "header-$status", contentType = "header") {
                        Row(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionHeader(
                                text = stringResource(
                                    R.string.feature_items_section_header,
                                    status.label(),
                                    section.itemCount,
                                ),
                                color = status.headerColor(),
                                modifier = Modifier.weight(1f),
                            )
                            if (status == ExpiryStatus.EXPIRED) {
                                AddExpiredToListButton(onClick = { onAddToShoppingList(expired) })
                            }
                        }
                    }
                }
                itemGroups(
                    groups = section.groups,
                    expandedGroups = expandedGroups,
                    selection = state.selection,
                    today = overview.today,
                    onGroupClick = onGroupClick,
                    onItemClick = onItemClick,
                    onFinish = onFinish,
                    onAddToShoppingList = onAddToShoppingList,
                    onToggleSelection = onToggleSelection,
                )
            }
        }
    }
}

/**
 * Shows a product with one item as that item, and one with several as a row that opens to show
 * each of them, soonest to expire first. A long press selects an item, or all of a product's
 * items; while there's a [selection], tapping selects or unselects them too.
 */
private fun LazyListScope.itemGroups(
    groups: List<ItemGroup>,
    expandedGroups: Set<String>,
    selection: Set<Long>,
    today: LocalDate,
    onGroupClick: (String) -> Unit,
    onItemClick: (Item) -> Unit,
    onFinish: (Item, ItemStatus) -> Unit,
    onAddToShoppingList: (List<Item>) -> Unit,
    onToggleSelection: (List<Item>) -> Unit,
) {
    val selecting = selection.isNotEmpty()
    fun ItemWithExpiry.selectionState() = if (selecting) ToggleableState(item.id in selection) else null
    fun ItemWithExpiry.snoozedUntil() = item.remindersSnoozedUntil?.takeIf { item.isReminderSnoozed(today) }

    groups.forEach { group ->
        // A product with one item shows as that item; the user's groups always show as a group.
        val single = group.entries.singleOrNull()?.takeIf { group.customGroup == null }
        if (single != null) {
            item(key = single.item.id, contentType = "item") {
                val select = { onToggleSelection(listOf(single.item)) }
                SwipeableItemRow(
                    onFinish = { outcome -> onFinish(single.item, outcome) },
                    onAddToShoppingList = { onAddToShoppingList(listOf(single.item)) },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    enabled = !selecting,
                ) {
                    ItemRow(
                        entry = single,
                        onClick = if (selecting) {
                            select
                        } else {
                            { onItemClick(single.item) }
                        },
                        onLongClick = select,
                        selection = single.selectionState(),
                        snoozedUntil = single.snoozedUntil(),
                    )
                }
            }
            return@forEach
        }

        val expanded = group.key in expandedGroups
        item(key = "group-${group.key}", contentType = "group") {
            val items = group.entries.map { it.item }
            val select = { onToggleSelection(items) }
            val selectedCount = items.count { it.id in selection }
            val shoppingLabel = stringResource(R.string.feature_items_add_to_shopping_list)
            ItemGroupRow(
                group = group,
                expanded = expanded,
                onClick = if (selecting) {
                    select
                } else {
                    { onGroupClick(group.key) }
                },
                onLongClick = select,
                selection = when {
                    !selecting -> null
                    selectedCount == items.size -> ToggleableState.On
                    selectedCount > 0 -> ToggleableState.Indeterminate
                    else -> ToggleableState.Off
                },
                snoozed = group.entries.all { it.item.isReminderSnoozed(today) },
                modifier = Modifier
                    .animateItem()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(shoppingLabel) {
                                onAddToShoppingList(items)
                                true
                            },
                        )
                    },
            )
        }
        if (expanded) {
            // Only what tells the items apart: shared details are on the group's row.
            val showBrand = group.entries.distinctBy { it.item.brand }.size > 1
            val showLocation = group.entries.distinctBy { it.item.location }.size > 1
            items(group.entries, key = { it.item.id }, contentType = { "group-item" }) { entry ->
                val select = { onToggleSelection(listOf(entry.item)) }
                SwipeableItemRow(
                    onFinish = { outcome -> onFinish(entry.item, outcome) },
                    onAddToShoppingList = { onAddToShoppingList(listOf(entry.item)) },
                    modifier = Modifier
                        .animateItem()
                        .padding(start = 40.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                    enabled = !selecting,
                ) {
                    val open = { onItemClick(entry.item) }
                    if (group.customGroup != null) {
                        // Items of different products, told apart by everything about them.
                        ItemRow(
                            entry = entry,
                            onClick = if (selecting) select else open,
                            onLongClick = select,
                            selection = entry.selectionState(),
                            snoozedUntil = entry.snoozedUntil(),
                        )
                    } else {
                        GroupEntryRow(
                            entry = entry,
                            showBrand = showBrand,
                            showLocation = showLocation,
                            onClick = if (selecting) select else open,
                            onLongClick = select,
                            selection = entry.selectionState(),
                            snoozedUntil = entry.snoozedUntil(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoMatchingItems(query: ItemQuery, onClearFilters: () -> Unit) {
    val (emoji, title) = when {
        query.searchText.isBlank() && query.categoryId == null && query.locationId == null &&
            query.status == ExpiryStatus.EXPIRED -> "🎉" to stringResource(R.string.feature_items_nothing_expired)

        else -> "🔍" to stringResource(R.string.feature_items_no_results_title)
    }
    EmptyState(
        emoji = emoji,
        title = title,
        message = stringResource(R.string.feature_items_no_results_message),
        action = {
            TextButton(onClick = onClearFilters) { Text(stringResource(R.string.feature_items_clear_filters)) }
        },
    )
}

private val FAB_CLEARANCE = 152.dp

/** Puts the products of everything expired on the shopping list. */
@Composable
private fun AddExpiredToListButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.feature_items_add_expired_to_list), modifier = Modifier.padding(start = 8.dp))
    }
}
