package io.github.abhik9.expirywatch.feature.shopping.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.core.navigation.ShoppingNavKey
import io.github.abhik9.expirywatch.feature.shopping.ShoppingRoute

fun EntryProviderScope<NavKey>.shoppingEntry() {
    entry<ShoppingNavKey> {
        ShoppingRoute(viewModel = hiltViewModel())
    }
}
