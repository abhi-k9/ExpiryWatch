package io.github.abhik9.expirywatch.feature.settings.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.core.navigation.AdvancedSettingsNavKey
import io.github.abhik9.expirywatch.core.navigation.ManageLabelsNavKey
import io.github.abhik9.expirywatch.core.navigation.Navigator
import io.github.abhik9.expirywatch.core.navigation.SettingsNavKey
import io.github.abhik9.expirywatch.feature.settings.SettingsRoute
import io.github.abhik9.expirywatch.feature.settings.advanced.AdvancedSettingsRoute
import io.github.abhik9.expirywatch.feature.settings.labels.ManageLabelsRoute
import io.github.abhik9.expirywatch.feature.settings.labels.ManageLabelsViewModel

fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsNavKey> {
        SettingsRoute(
            onManageLabels = { kind -> navigator.navigate(ManageLabelsNavKey(kind)) },
            onOpenAdvanced = { navigator.navigate(AdvancedSettingsNavKey) },
            viewModel = hiltViewModel(),
        )
    }
    entry<AdvancedSettingsNavKey> {
        AdvancedSettingsRoute(onBack = { navigator.goBack() }, viewModel = hiltViewModel())
    }
    entry<ManageLabelsNavKey> { key ->
        ManageLabelsRoute(
            onBack = { navigator.goBack() },
            viewModel = hiltViewModel<ManageLabelsViewModel, ManageLabelsViewModel.Factory> { factory ->
                factory.create(key.kind)
            },
        )
    }
}
