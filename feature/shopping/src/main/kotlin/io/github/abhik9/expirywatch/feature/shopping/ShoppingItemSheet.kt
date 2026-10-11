package io.github.abhik9.expirywatch.feature.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.domain.shopping.isValidShoppingQuantity
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.ui.PickerField
import io.github.abhik9.expirywatch.core.ui.label
import io.github.abhik9.expirywatch.core.ui.parseQuantity
import io.github.abhik9.expirywatch.core.ui.toQuantityText
import kotlinx.coroutines.launch

/** Changes an item on the list: its name, amount, category and note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShoppingItemSheet(
    item: ShoppingItem,
    categories: List<Category>,
    onSave: (ShoppingItem) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Slides the sheet away before acting, rather than making it vanish, and acts only once.
    var acted by remember { mutableStateOf(false) }
    val hideThen = { action: () -> Unit ->
        if (!acted) {
            acted = true
            scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
        }
    }

    var name by rememberSaveable { mutableStateOf(item.name) }
    var amount by rememberSaveable { mutableStateOf(item.quantity.toQuantityText()) }
    var unit by rememberSaveable { mutableStateOf(item.unit) }
    var categoryId by rememberSaveable { mutableStateOf(item.category?.id) }
    var note by rememberSaveable { mutableStateOf(item.note.orEmpty()) }
    val quantity = amount.parseQuantity()?.takeIf(::isValidShoppingQuantity)
    val canSave = name.isNotBlank() && quantity != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.feature_shopping_edit_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.feature_shopping_name)) },
                isError = name.isBlank(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.feature_shopping_amount)) },
                    isError = quantity == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                )
                val unitLabels = QuantityUnit.entries.associateWith { it.label() }
                PickerField(
                    label = stringResource(R.string.feature_shopping_unit),
                    selectedText = unitLabels.getValue(unit),
                    noneText = null,
                    options = QuantityUnit.entries.map { it.ordinal.toLong() to unitLabels.getValue(it) },
                    onSelect = { ordinal -> unit = QuantityUnit.entries[(ordinal ?: 0L).toInt()] },
                    modifier = Modifier.weight(1f),
                )
            }
            PickerField(
                label = stringResource(R.string.feature_shopping_category),
                selectedText = categories.firstOrNull { it.id == categoryId }?.label(),
                noneText = stringResource(R.string.feature_shopping_none),
                options = categories.map { it.id to it.label() },
                onSelect = { categoryId = it },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.feature_shopping_note)) },
                placeholder = { Text(stringResource(R.string.feature_shopping_note_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { hideThen(onRemove) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.feature_shopping_remove))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        val changed = item.copy(
                            name = name,
                            quantity = checkNotNull(quantity),
                            unit = unit,
                            // A category deleted meanwhile is left out rather than saved.
                            category = categories.firstOrNull { it.id == categoryId },
                            note = note,
                        )
                        hideThen { onSave(changed) }
                    },
                    enabled = canSave,
                ) {
                    Text(stringResource(R.string.feature_shopping_save))
                }
            }
        }
    }
}
