package io.github.abhik9.expirywatch.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * A read-only field that opens a bounded, scrollable picker near the bottom of the screen.
 *
 * @param noneText when not null, an extra first option that selects `null`.
 * @param addText when not null, a pinned action, such as "New category…", that calls [onAdd].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerField(
    label: String,
    selectedText: String?,
    noneText: String?,
    options: List<Pair<Long, String>>,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    addText: String? = null,
    onAdd: () -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedText ?: noneText.orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            singleLine = true,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .semantics {
                    contentDescription = label
                    stateDescription = selectedText ?: noneText.orEmpty()
                }
                .clickable(role = Role.DropdownList, onClickLabel = label) {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    expanded = true
                },
        )
    }
    if (expanded) {
        val selectedIndex = options.indexOfFirst { it.second == selectedText }
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = (selectedIndex + (if (noneText != null) 1 else 0) - 2).coerceAtLeast(0),
        )
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.65f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false), state = listState) {
                    if (noneText != null) {
                        item {
                            PickerOption(noneText, selectedText == null) {
                                expanded = false
                                onSelect(null)
                            }
                        }
                    }
                    items(options, key = { it.first }) { (id, text) ->
                        PickerOption(text, text == selectedText) {
                            expanded = false
                            onSelect(id)
                        }
                    }
                }
                if (addText != null) {
                    HorizontalDivider()
                    TextButton(
                        onClick = {
                            expanded = false
                            onAdd()
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(addText, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = { if (isSelected) Icon(Icons.Filled.Check, contentDescription = null) },
        modifier = Modifier
            .semantics { selected = isSelected }
            .clickable(role = Role.RadioButton, onClick = onClick),
    )
}
