package io.github.abhik9.expirywatch.feature.shopping.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.core.navigation.ItemsNavKey
import io.github.abhik9.expirywatch.core.navigation.Navigator
import io.github.abhik9.expirywatch.core.navigation.PutAwayNavKey
import io.github.abhik9.expirywatch.core.navigation.ShoppingNavKey
import io.github.abhik9.expirywatch.feature.shopping.ShoppingRoute
import io.github.abhik9.expirywatch.feature.shopping.putaway.PutAwayRoute
import io.github.abhik9.expirywatch.feature.shopping.putaway.PutAwayViewModel

fun EntryProviderScope<NavKey>.shoppingEntries(navigator: Navigator) {
    entry<ShoppingNavKey> {
        ShoppingRoute(
            onPutAway = { listId -> navigator.navigate(PutAwayNavKey(listId)) },
            viewModel = hiltViewModel(),
        )
    }
    entry<PutAwayNavKey> { key ->
        PutAwayRoute(
            onClose = { navigator.goBack() },
            onShowItems = { navigator.navigate(ItemsNavKey()) },
            viewModel = hiltViewModel<PutAwayViewModel, PutAwayViewModel.Factory> { factory ->
                factory.create(listId = key.listId)
            },
        )
    }
}
