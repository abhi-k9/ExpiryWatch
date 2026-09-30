package io.github.abhik9.expirywatch.feature.editor.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.expirywatch.core.navigation.EditorNavKey
import io.github.abhik9.expirywatch.core.navigation.Navigator
import io.github.abhik9.expirywatch.feature.editor.EditorRoute
import io.github.abhik9.expirywatch.feature.editor.EditorViewModel

fun EntryProviderScope<NavKey>.editorEntry(navigator: Navigator) {
    entry<EditorNavKey> { key ->
        EditorRoute(
            openScanner = key.scanBarcode && key.itemId == null,
            onDone = { navigator.goBack() },
            viewModel = hiltViewModel<EditorViewModel, EditorViewModel.Factory> { factory -> factory.create(key) },
        )
    }
}
