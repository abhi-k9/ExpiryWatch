package io.github.abhik9.expirywatch.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Emoji to offer for each kind of label. */
object LabelEmojis {
    val CATEGORIES = listOf(
        "🧀", "🥛", "🥚", "🍗", "🐟", "🥕", "🍎", "🍞", "🥫", "🍝", "🍨", "🧃", "🍷", "🍫", "🧂", "🍯",
        "☕", "🍼", "💊", "🩹", "🧴", "🐶", "🧽", "📦",
    )

    val LOCATIONS = listOf(
        "🧊", "❄️", "🗄️", "🍽️", "🪥", "🛁", "🏠", "🚗", "🧺", "📦", "🧰", "🛒",
    )

    val GROUPS = listOf(
        "🗂️", "⛺", "🎒", "🧳", "🍼", "🐾", "🎉", "🎄", "🍱", "🥗", "🏋️", "💊", "🧰", "🏠", "🚗", "📦",
    )
}

/**
 * Asks for the name and emoji of a category, location or group, offering [suggestions] for the
 * emoji. [onConfirm] gets the name as typed.
 */
@Composable
fun LabelDialog(
    title: String,
    confirmText: String,
    suggestions: List<String>,
    onConfirm: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialEmoji: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var emoji by rememberSaveable { mutableStateOf(initialEmoji.ifEmpty { suggestions.first() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.core_ui_label_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(MAX_EMOJI_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.core_ui_label_emoji)) },
                    singleLine = true,
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 44.dp),
                    modifier = Modifier.heightIn(max = 160.dp),
                ) {
                    items(suggestions) { suggestion ->
                        Box(
                            modifier = Modifier
                                .clickable { emoji = suggestion }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(suggestion, fontSize = 24.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, emoji) }, enabled = name.isNotBlank()) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_ui_cancel)) }
        },
    )
}

// Emoji can be several code units long (e.g. with a variation selector or skin tone).
private const val MAX_EMOJI_LENGTH = 8
