package io.github.abhik9.expirywatch.feature.settings.labels

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.EmojiAvatar
import io.github.abhik9.expirywatch.core.designsystem.component.EmptyState
import io.github.abhik9.expirywatch.core.navigation.LabelKind
import io.github.abhik9.expirywatch.feature.settings.R

@Composable
internal fun ManageLabelsRoute(onBack: () -> Unit, viewModel: ManageLabelsViewModel) {
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    ManageLabelsScreen(
        kind = viewModel.kind,
        labels = labels,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageLabelsScreen(
    kind: LabelKind,
    labels: List<Label>?,
    onSave: (Label) -> Unit,
    onDelete: (Label) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The label being edited: a new one has id 0.
    var editing by remember { mutableStateOf<Label?>(null) }
    var deleting by remember { mutableStateOf<Label?>(null) }
    val isCategories = kind == LabelKind.CATEGORIES
    val strings = if (isCategories) CategoryStrings else LocationStrings

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(strings.title),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_settings_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = Label(name = "", emoji = "") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = {
                    Text(
                        stringResource(strings.add),
                    )
                },
            )
        },
    ) { innerPadding ->
        when {
            labels == null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            labels.isEmpty() -> EmptyState(
                emoji = if (isCategories) "🏷️" else "🗄️",
                title = stringResource(R.string.feature_settings_labels_empty_title),
                message = stringResource(R.string.feature_settings_labels_empty_message),
                modifier = Modifier.padding(innerPadding),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                items(labels, key = { it.id }) { label ->
                    ListItem(
                        headlineContent = { Text(label.name) },
                        leadingContent = { EmojiAvatar(emoji = label.emoji, size = 40.dp) },
                        trailingContent = {
                            IconButton(onClick = { deleting = label }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = stringResource(
                                        R.string.feature_settings_delete_label,
                                        label.name,
                                    ),
                                )
                            }
                        },
                        modifier = Modifier
                            .animateItem()
                            .clickable(
                                onClickLabel = stringResource(R.string.feature_settings_edit),
                            ) { editing = label },
                    )
                }
            }
        }
    }

    editing?.let { label ->
        LabelEditorDialog(
            initial = label,
            suggestions = if (isCategories) CATEGORY_EMOJIS else LOCATION_EMOJIS,
            onSave = {
                onSave(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { label ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.feature_settings_delete_label_title, label.name)) },
            text = {
                Text(
                    stringResource(strings.deleteMessage),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(label)
                        deleting = null
                    },
                ) { Text(stringResource(R.string.feature_settings_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.feature_settings_cancel)) }
            },
        )
    }
}

@Composable
private fun LabelEditorDialog(
    initial: Label,
    suggestions: List<String>,
    onSave: (Label) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var emoji by remember { mutableStateOf(initial.emoji.ifEmpty { suggestions.first() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial.id == 0L) R.string.feature_settings_new_label else R.string.feature_settings_edit_label,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.feature_settings_label_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(MAX_EMOJI_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.feature_settings_label_emoji)) },
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
            TextButton(
                onClick = { onSave(initial.copy(name = name, emoji = emoji)) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.feature_settings_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_settings_cancel)) }
        },
    )
}

private class LabelStrings(
    @param:StringRes val title: Int,
    @param:StringRes val add: Int,
    @param:StringRes val deleteMessage: Int,
)

private val CategoryStrings = LabelStrings(
    title = R.string.feature_settings_categories,
    add = R.string.feature_settings_add_category,
    deleteMessage = R.string.feature_settings_delete_category_message,
)

private val LocationStrings = LabelStrings(
    title = R.string.feature_settings_locations,
    add = R.string.feature_settings_add_location,
    deleteMessage = R.string.feature_settings_delete_location_message,
)

// Emoji can be several code units long (e.g. with a variation selector or skin tone).
private const val MAX_EMOJI_LENGTH = 8

private val CATEGORY_EMOJIS = listOf(
    "🧀", "🥛", "🥚", "🍗", "🐟", "🥕", "🍎", "🍞", "🥫", "🍝", "🍨", "🧃", "🍷", "🍫", "🧂", "🍯",
    "☕", "🍼", "💊", "🩹", "🧴", "🐶", "🧽", "📦",
)

private val LOCATION_EMOJIS = listOf(
    "🧊", "❄️", "🗄️", "🍽️", "🪥", "🛁", "🏠", "🚗", "🧺", "📦", "🧰", "🛒",
)
