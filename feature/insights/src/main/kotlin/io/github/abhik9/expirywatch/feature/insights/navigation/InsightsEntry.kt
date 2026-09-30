package io.github.abhik9.expirywatch.feature.insights.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.core.navigation.InsightsNavKey
import io.github.abhik9.expirywatch.feature.insights.InsightsRoute

fun EntryProviderScope<NavKey>.insightsEntry() {
    entry<InsightsNavKey> {
        InsightsRoute(viewModel = hiltViewModel())
    }
}
