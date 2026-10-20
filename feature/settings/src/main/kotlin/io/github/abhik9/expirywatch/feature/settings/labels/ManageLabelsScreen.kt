package io.github.abhik9.expirywatch.feature.settings.labels

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.EmojiAvatar
import io.github.abhik9.expirywatch.core.designsystem.component.EmptyState
import io.github.abhik9.expirywatch.core.navigation.LabelKind
import io.github.abhik9.expirywatch.core.ui.LabelDialog
import io.github.abhik9.expirywatch.core.ui.LabelEmojis
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
    val strings = when (kind) {
        LabelKind.CATEGORIES -> CategoryStrings
        LabelKind.LOCATIONS -> LocationStrings
        LabelKind.GROUPS -> GroupStrings
    }

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
                emoji = strings.emptyEmoji,
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
        LabelDialog(
            title = stringResource(
                if (label.id == 0L) R.string.feature_settings_new_label else R.string.feature_settings_edit_label,
            ),
            confirmText = stringResource(R.string.feature_settings_save),
            suggestions = strings.suggestions,
            onConfirm = { name, emoji ->
                onSave(label.copy(name = name, emoji = emoji))
                editing = null
            },
            onDismiss = { editing = null },
            initialName = label.name,
            initialEmoji = label.emoji,
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

private class LabelStrings(
    @param:StringRes val title: Int,
    @param:StringRes val add: Int,
    @param:StringRes val deleteMessage: Int,
    val emptyEmoji: String,
    val suggestions: List<String>,
)

private val CategoryStrings = LabelStrings(
    title = R.string.feature_settings_categories,
    add = R.string.feature_settings_add_category,
    deleteMessage = R.string.feature_settings_delete_category_message,
    emptyEmoji = "🏷️",
    suggestions = LabelEmojis.CATEGORIES,
)

private val LocationStrings = LabelStrings(
    title = R.string.feature_settings_locations,
    add = R.string.feature_settings_add_location,
    deleteMessage = R.string.feature_settings_delete_location_message,
    emptyEmoji = "🗄️",
    suggestions = LabelEmojis.LOCATIONS,
)

private val GroupStrings = LabelStrings(
    title = R.string.feature_settings_groups,
    add = R.string.feature_settings_add_group,
    deleteMessage = R.string.feature_settings_delete_group_message,
    emptyEmoji = "🗂️",
    suggestions = LabelEmojis.GROUPS,
)
