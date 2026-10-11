package io.github.abhik9.expirywatch.feature.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.designsystem.theme.ExpiryWatchTheme
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListResult
import io.github.abhik9.expirywatch.core.domain.shopping.KnownProduct
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingEntry
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.ui.formatQuantity
import java.time.Instant

/**
 * Where items are typed in. Done adds the item and keeps the keyboard open for the next one. Below
 * the field: what happened to the last item added, what's about to be added when it has an
 * amount, or else a tip on typing one.
 */
@Composable
internal fun AddItemField(
    input: String,
    lastAdded: AddToShoppingListResult?,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typed = ShoppingEntry.parse(input)
    val feedback = if (typed == null) lastAdded?.message() else null
    val supportingText = when {
        feedback != null -> feedback

        typed != null && typed.hasAmount -> stringResource(
            R.string.feature_shopping_preview,
            typed.name,
            formatQuantity(typed.quantity, typed.unit),
        )

        else -> stringResource(R.string.feature_shopping_amount_tip)
    }
    OutlinedTextField(
        value = input,
        onValueChange = onInputChange,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.feature_shopping_add_hint)) },
        trailingIcon = if (typed != null) {
            {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.feature_shopping_add))
                }
            }
        } else {
            null
        },
        supportingText = {
            Text(
                text = supportingText,
                // Only what happened is announced, not each change while typing.
                modifier = if (feedback != null) {
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                } else {
                    Modifier
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { if (typed == null) defaultKeyboardAction(ImeAction.Done) else onAdd() },
        ),
    )
}

private val ShoppingEntry.hasAmount: Boolean get() = quantity != 1.0 || unit != QuantityUnit.PIECES

@Composable
private fun AddToShoppingListResult.message(): String = when (this) {
    is AddToShoppingListResult.Added -> item.category?.let { category ->
        stringResource(R.string.feature_shopping_added_to, item.name, category.label())
    } ?: stringResource(R.string.feature_shopping_added, item.name)

    is AddToShoppingListResult.AlreadyOnList -> stringResource(
        R.string.feature_shopping_already_on_list,
        item.name,
        formatQuantity(item.quantity, item.unit),
    )

    is AddToShoppingListResult.BackOnList -> stringResource(R.string.feature_shopping_back_on_list, item.name)
}

internal fun Category.label(): String = "$emoji $name"

/** Products to add with a tap: those matching what's typed, or the ones added most often. */
@Composable
internal fun SuggestionRow(
    suggestions: List<KnownProduct>,
    onClick: (KnownProduct) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(suggestions, key = { it.key }) { product ->
            SuggestionChip(
                onClick = { onClick(product) },
                label = { Text(product.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.animateItem(),
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(SuggestionChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

/** Something to buy, ticked off once it's in the cart. The whole row toggles it. */
@Composable
internal fun ShoppingItemRow(
    item: ShoppingItem,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = if (item.isChecked) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = item.isChecked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val details = listOfNotNull(item.brand, item.note).joinToString(separator = " · ")
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (item.quantity != 1.0 || item.unit != QuantityUnit.PIECES) {
            Text(
                text = formatQuantity(item.quantity, item.unit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun CartHeader(
    count: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionHeader(
            text = stringResource(R.string.feature_shopping_in_cart, count),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.feature_shopping_clear_cart))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingItemRowPreview() {
    ExpiryWatchTheme(dynamicColor = false) {
        Column {
            ShoppingItemRow(
                item = ShoppingItem(listId = 1, name = "Flour", quantity = 1.5, unit = QuantityUnit.KILOGRAMS),
                onCheckedChange = {},
            )
            ShoppingItemRow(
                item = ShoppingItem(listId = 1, name = "Milk", note = "Lactose-free", checkedAt = Instant.EPOCH),
                onCheckedChange = {},
            )
        }
    }
}
