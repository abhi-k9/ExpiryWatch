package io.github.abhik9.expirywatch.feature.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.EmptyState
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.domain.usecase.ItemQuery
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
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
    viewModel: ItemsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ItemsEvent.ItemFinished -> {
                    val messageRes = when (event.outcome) {
                        ItemStatus.WASTED -> R.string.feature_items_marked_wasted
                        else -> R.string.feature_items_marked_consumed
                    }
                    val message = context.getString(messageRes, event.item.name)
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = context.getString(R.string.feature_items_undo),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoFinish(event.item)
                }
            }
        }
    }

    ItemsScreen(
        uiState = uiState,
        searchText = viewModel.searchText,
        snackbarHostState = snackbarHostState,
        onSearchTextChange = viewModel::onSearchTextChange,
        onStatusFilterChange = viewModel::onStatusFilterChange,
        onCategoryFilterChange = viewModel::onCategoryFilterChange,
        onLocationFilterChange = viewModel::onLocationFilterChange,
        onClearFilters = viewModel::clearFilters,
        onSortOrderChange = viewModel::onSortOrderChange,
        onItemClick = onItemClick,
        onFinish = viewModel::finish,
        onAddItem = onAddItem,
        onScanItem = onScanItem,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemsScreen(
    uiState: ItemsUiState,
    searchText: String,
    snackbarHostState: SnackbarHostState,
    onSearchTextChange: (String) -> Unit,
    onStatusFilterChange: (ExpiryStatus?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onLocationFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onSortOrderChange: (ItemSortOrder) -> Unit,
    onItemClick: (Item) -> Unit,
    onFinish: (Item, ItemStatus) -> Unit,
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
                contentPadding = innerPadding,
                onStatusFilterChange = onStatusFilterChange,
                onCategoryFilterChange = onCategoryFilterChange,
                onLocationFilterChange = onLocationFilterChange,
                onClearFilters = onClearFilters,
                onItemClick = onItemClick,
                onFinish = onFinish,
                onAddItem = onAddItem,
            )
        }
    }
}

@Composable
private fun ItemsContent(
    state: ItemsUiState.Success,
    contentPadding: PaddingValues,
    onStatusFilterChange: (ExpiryStatus?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onLocationFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onItemClick: (Item) -> Unit,
    onFinish: (Item, ItemStatus) -> Unit,
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
        } else if (overview.sortOrder.groupsByStatus && state.query.status == null) {
            overview.items.groupBy { it.status }.forEach { (status, itemsInGroup) ->
                item(key = "header-$status", contentType = "header") {
                    SectionHeader(
                        text = stringResource(R.string.feature_items_section_header, status.label(), itemsInGroup.size),
                        color = status.headerColor(),
                        modifier = Modifier.animateItem(),
                    )
                }
                itemRows(itemsInGroup, onItemClick, onFinish)
            }
        } else {
            itemRows(overview.items, onItemClick, onFinish)
        }
    }
}

private fun LazyListScope.itemRows(
    items: List<ItemWithExpiry>,
    onItemClick: (Item) -> Unit,
    onFinish: (Item, ItemStatus) -> Unit,
) {
    items(items, key = { it.item.id }, contentType = { "item" }) { entry ->
        SwipeableItemRow(
            entry = entry,
            onClick = { onItemClick(entry.item) },
            onFinish = { outcome -> onFinish(entry.item, outcome) },
            modifier = Modifier
                .animateItem()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
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

private val ItemSortOrder.groupsByStatus: Boolean
    get() = this == ItemSortOrder.EXPIRY_SOONEST || this == ItemSortOrder.EXPIRY_LATEST

private val FAB_CLEARANCE = 152.dp
