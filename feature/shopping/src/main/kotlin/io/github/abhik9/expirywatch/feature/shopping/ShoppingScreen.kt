package io.github.abhik9.expirywatch.feature.shopping

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.EmptyState
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListResult
import io.github.abhik9.expirywatch.core.domain.shopping.KnownProduct
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingListOverview
import io.github.abhik9.expirywatch.core.model.ShoppingItem

@Composable
internal fun ShoppingRoute(viewModel: ShoppingViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Updated on configuration changes without restarting the effect, which would drop a shown snackbar.
    val resources by rememberUpdatedState(LocalResources.current)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ShoppingEvent.ItemRemoved -> {
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.feature_shopping_removed, event.item.name),
                        actionLabel = resources.getString(R.string.feature_shopping_undo),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove(event.item)
                }

                is ShoppingEvent.CartCleared -> {
                    val count = event.items.size
                    val result = snackbarHostState.showSnackbar(
                        message = resources.getQuantityString(R.plurals.feature_shopping_cart_cleared, count, count),
                        actionLabel = resources.getString(R.string.feature_shopping_undo),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoClearCart(event.items)
                }
            }
        }
    }

    ShoppingScreen(
        uiState = uiState,
        input = viewModel.input,
        lastAdded = viewModel.lastAdded,
        snackbarHostState = snackbarHostState,
        onInputChange = viewModel::onInputChange,
        onAdd = viewModel::add,
        onAddSuggestion = viewModel::addSuggestion,
        onCheckedChange = viewModel::onCheckedChange,
        onEdit = viewModel::onEdit,
        onRemove = viewModel::remove,
        onClearCart = viewModel::clearCart,
    )

    val editing = viewModel.editing
    val categories = (uiState as? ShoppingUiState.Success)?.categories.orEmpty()
    if (editing != null) {
        // A sheet of its own for each item, so it doesn't keep what was typed for another.
        key(editing.id) {
            ShoppingItemSheet(
                item = editing,
                categories = categories,
                onSave = viewModel::save,
                onRemove = { viewModel.remove(editing) },
                onDismiss = viewModel::onEditDismiss,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShoppingScreen(
    uiState: ShoppingUiState,
    input: String,
    lastAdded: AddToShoppingListResult?,
    snackbarHostState: SnackbarHostState,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onAddSuggestion: (KnownProduct) -> Unit,
    onCheckedChange: (ShoppingItem, Boolean) -> Unit,
    onEdit: (ShoppingItem) -> Unit,
    onRemove: (ShoppingItem) -> Unit,
    onClearCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text((uiState as? ShoppingUiState.Success)?.overview?.list?.name.orEmpty()) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            ShoppingUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is ShoppingUiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                AddItemField(
                    input = input,
                    lastAdded = lastAdded,
                    onInputChange = onInputChange,
                    onAdd = onAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                SuggestionRow(suggestions = uiState.suggestions, onClick = onAddSuggestion)
                if (uiState.overview.isEmpty) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = innerPadding.calculateBottomPadding()),
                    ) {
                        EmptyState(
                            emoji = "🛒",
                            title = stringResource(R.string.feature_shopping_empty_title),
                            message = stringResource(R.string.feature_shopping_empty_message),
                        )
                    }
                } else {
                    ShoppingItems(
                        overview = uiState.overview,
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        onCheckedChange = onCheckedChange,
                        onEdit = onEdit,
                        onRemove = onRemove,
                        onClearCart = onClearCart,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** What's still to buy under a header per category, then what's in the cart. */
@Composable
private fun ShoppingItems(
    overview: ShoppingListOverview,
    bottomPadding: Dp,
    onCheckedChange: (ShoppingItem, Boolean) -> Unit,
    onEdit: (ShoppingItem) -> Unit,
    onRemove: (ShoppingItem) -> Unit,
    onClearCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A single "No category" header would only take up room.
    val showAisleHeaders = overview.aisles.any { it.category != null }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 16.dp),
    ) {
        overview.aisles.forEach { aisle ->
            if (showAisleHeaders) {
                item(key = "aisle-${aisle.category?.id}", contentType = "aisle") {
                    SectionHeader(
                        text = aisle.category?.label() ?: stringResource(R.string.feature_shopping_no_category),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            items(aisle.items, key = { it.id }, contentType = { "item" }) { item ->
                ShoppingItemRow(
                    item = item,
                    onCheckedChange = { checked -> onCheckedChange(item, checked) },
                    onEdit = { onEdit(item) },
                    onRemove = { onRemove(item) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (overview.inCart.isNotEmpty()) {
            item(key = "cart", contentType = "cart") {
                CartHeader(count = overview.inCart.size, onClear = onClearCart, modifier = Modifier.animateItem())
            }
            items(overview.inCart, key = { it.id }, contentType = { "item" }) { item ->
                ShoppingItemRow(
                    item = item,
                    onCheckedChange = { checked -> onCheckedChange(item, checked) },
                    onEdit = { onEdit(item) },
                    onRemove = { onRemove(item) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}
