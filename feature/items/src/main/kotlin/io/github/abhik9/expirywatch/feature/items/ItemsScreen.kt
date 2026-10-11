package io.github.abhik9.expirywatch.feature.items

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.EmptyState
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.domain.usecase.ItemGroup
import io.github.abhik9.expirywatch.core.domain.usecase.ItemQuery
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.ui.label

@Composable
internal fun ItemsRoute(
    onItemClick: (Item) -> Unit,
    onAddItem: () -> Unit,
    onScanItem: () -> Unit,
    onOpenShoppingList: () -> Unit,
    viewModel: ItemsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    )
}

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
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val sortOrder = (uiState as? ItemsUiState.Success)?.overview?.sortOrder ?: ItemSortOrder.EXPIRY_SOONEST

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ItemsTopBar(
                searchText = searchText,
                onSearchTextChange = onSearchTextChange,
                sortOrder = sortOrder,
                onSortOrderChange = onSortOrderChange,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SmallFloatingActionButton(onClick = onScanItem) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = stringResource(R.string.feature_items_scan))
                }
                ExtendedFloatingActionButton(
                    onClick = onAddItem,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.feature_items_add_item)) },
                )
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
            )
        }
    }
}

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
                itemGroups(section.groups, expandedGroups, onGroupClick, onItemClick, onFinish, onAddToShoppingList)
            }
        }
    }
}

/**
 * Shows a product with one item as that item, and one with several as a row that opens to show
 * each of them, soonest to expire first.
 */
private fun LazyListScope.itemGroups(
    groups: List<ItemGroup>,
    expandedGroups: Set<String>,
    onGroupClick: (String) -> Unit,
    onItemClick: (Item) -> Unit,
    onFinish: (Item, ItemStatus) -> Unit,
    onAddToShoppingList: (List<Item>) -> Unit,
) {
    groups.forEach { group ->
        val single = group.entries.singleOrNull()
        if (single != null) {
            item(key = single.item.id, contentType = "item") {
                val addToShoppingList = { onAddToShoppingList(listOf(single.item)) }
                SwipeableItemRow(
                    onFinish = { outcome -> onFinish(single.item, outcome) },
                    onAddToShoppingList = addToShoppingList,
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    ItemMenuBox(onAddToShoppingList = addToShoppingList) { openMenu ->
                        ItemRow(entry = single, onClick = { onItemClick(single.item) }, onLongClick = openMenu)
                    }
                }
            }
            return@forEach
        }

        val expanded = group.key in expandedGroups
        item(key = "group-${group.key}", contentType = "group") {
            val addToShoppingList = { onAddToShoppingList(group.entries.map { it.item }) }
            val shoppingLabel = stringResource(R.string.feature_items_add_to_shopping_list)
            ItemMenuBox(
                onAddToShoppingList = addToShoppingList,
                modifier = Modifier
                    .animateItem()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) { openMenu ->
                ItemGroupRow(
                    group = group,
                    expanded = expanded,
                    onClick = { onGroupClick(group.key) },
                    onLongClick = openMenu,
                    modifier = Modifier.semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(shoppingLabel) {
                                addToShoppingList()
                                true
                            },
                        )
                    },
                )
            }
        }
        if (expanded) {
            // Only what tells the items apart: shared details are on the group's row.
            val showBrand = group.entries.distinctBy { it.item.brand }.size > 1
            val showLocation = group.entries.distinctBy { it.item.location }.size > 1
            items(group.entries, key = { it.item.id }, contentType = { "group-item" }) { entry ->
                val addToShoppingList = { onAddToShoppingList(listOf(entry.item)) }
                SwipeableItemRow(
                    onFinish = { outcome -> onFinish(entry.item, outcome) },
                    onAddToShoppingList = addToShoppingList,
                    modifier = Modifier
                        .animateItem()
                        .padding(start = 40.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                ) {
                    ItemMenuBox(onAddToShoppingList = addToShoppingList) { openMenu ->
                        GroupEntryRow(
                            entry = entry,
                            showBrand = showBrand,
                            showLocation = showLocation,
                            onClick = { onItemClick(entry.item) },
                            onLongClick = openMenu,
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
