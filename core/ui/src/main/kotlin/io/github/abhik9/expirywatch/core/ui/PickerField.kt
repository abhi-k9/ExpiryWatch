package io.github.abhik9.expirywatch.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow

/**
 * A read-only field with a dropdown of [options] (id to label).
 *
 * @param noneText when not null, an extra first option that selects `null`.
 * @param addText when not null, a last option, such as "New category…", that calls [onAdd].
 */
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
    var expanded by remember { mutableStateOf(false) }
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
                .clickable(role = Role.DropdownList, onClickLabel = label) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (noneText != null) {
                DropdownMenuItem(
                    text = { Text(noneText) },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    },
                )
            }
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
            if (addText != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(addText) },
                    onClick = {
                        expanded = false
                        onAdd()
                    },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                )
            }
        }
    }
}
