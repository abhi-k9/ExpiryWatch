package io.github.abhik9.expirywatch.feature.shopping.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingEntry
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingListSummary
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.ui.PickerField
import io.github.abhik9.expirywatch.core.ui.formatQuantity
import io.github.abhik9.expirywatch.feature.shopping.R

@Composable
internal fun AddFromTextRoute(
    onClose: () -> Unit,
    viewModel: AddFromTextViewModel,
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AddFromTextEvent.Added -> onClose()
            }
        }
    }

    AddFromTextScreen(
        text = viewModel.text,
        entries = entries,
        lists = lists,
        listId = viewModel.listId,
        isAdding = viewModel.isAdding,
        onTextChange = viewModel::onTextChange,
        onListChange = viewModel::onListChange,
        onAdd = viewModel::add,
        onClose = onClose,
    )
}

/** Adds the items written in text, one per line, to a shopping list, showing what it reads as it's typed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddFromTextScreen(
    text: String,
    entries: List<ShoppingEntry>,
    lists: List<ShoppingListSummary>,
    listId: Long?,
    isAdding: Boolean,
    onTextChange: (String) -> Unit,
    onListChange: (Long) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_shopping_add_from_text)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_shopping_close))
                    }
                },
                actions = {
                    Button(
                        onClick = onAdd,
                        enabled = entries.isNotEmpty() && listId != null && !isAdding,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(stringResource(R.string.feature_shopping_add))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PickerField(
                label = stringResource(R.string.feature_shopping_add_to_label),
                selectedText = lists.firstOrNull { it.list.id == listId }?.list?.name,
                noneText = null,
                options = lists.map { it.list.id to it.list.name },
                onSelect = { id -> id?.let(onListChange) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.feature_shopping_text)) },
                placeholder = { Text(stringResource(R.string.feature_shopping_text_hint)) },
                minLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Text(
                text = if (entries.isEmpty()) {
                    stringResource(R.string.feature_shopping_nothing_to_add)
                } else {
                    pluralStringResource(R.plurals.feature_shopping_items_to_add, entries.size, entries.size)
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            entries.forEach { entry ->
                Text(text = entry.describe(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** The entry as it'll appear on the list, e.g. "🧀 Milk · 2 L · lactose-free". */
@Composable
private fun ShoppingEntry.describe(): String = listOfNotNull(
    listOfNotNull(category?.emoji, name).joinToString(" "),
    formatQuantity(quantity, unit).takeIf { quantity != 1.0 || unit != QuantityUnit.PIECES },
    note,
).joinToString(" · ")
