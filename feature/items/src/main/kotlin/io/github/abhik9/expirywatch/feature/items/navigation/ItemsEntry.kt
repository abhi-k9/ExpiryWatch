package io.github.abhik9.expirywatch.feature.items.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.core.navigation.EditorNavKey
import io.github.abhik9.expirywatch.core.navigation.ItemsNavKey
import io.github.abhik9.expirywatch.core.navigation.Navigator
import io.github.abhik9.expirywatch.feature.items.ItemsRoute
import io.github.abhik9.expirywatch.feature.items.ItemsViewModel

fun EntryProviderScope<NavKey>.itemsEntry(navigator: Navigator) {
    entry<ItemsNavKey> { key ->
        ItemsRoute(
            onItemClick = { item -> navigator.navigate(EditorNavKey(itemId = item.id)) },
            onAddItem = { navigator.navigate(EditorNavKey()) },
            onScanItem = { navigator.navigate(EditorNavKey(scanBarcode = true)) },
            viewModel = hiltViewModel<ItemsViewModel, ItemsViewModel.Factory> { factory ->
                factory.create(initialStatus = key.status)
            },
        )
    }
}
