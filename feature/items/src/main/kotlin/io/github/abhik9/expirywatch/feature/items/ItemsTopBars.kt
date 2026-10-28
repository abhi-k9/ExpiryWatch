package io.github.abhik9.expirywatch.feature.items

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.NotificationsPaused
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.ui.colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemsTopBar(
    sortOrder: ItemSortOrder,
    onSortOrderChange: (ItemSortOrder) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.feature_items_title)) },
        actions = { SortMenu(sortOrder, onSortOrderChange) },
        scrollBehavior = scrollBehavior,
    )
}

/** Stands in for the top bar while items are selected, with what can be done to all of them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onSnooze: () -> Unit,
    onAddToShoppingList: () -> Unit,
    onMoveToGroup: () -> Unit,
    onSelectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    BackHandler(onBack = onClose)
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.feature_items_selected, count, count)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_items_clear_selection))
            }
        },
        actions = {
            IconButton(onClick = onSnooze) {
                Icon(
                    Icons.Outlined.NotificationsPaused,
                    contentDescription = stringResource(R.string.feature_items_snooze_reminders),
                )
            }
            IconButton(onClick = onAddToShoppingList) {
                Icon(
                    Icons.Outlined.AddShoppingCart,
                    contentDescription = stringResource(R.string.feature_items_add_to_shopping_list),
                )
            }
            IconButton(onClick = onMoveToGroup) {
                Icon(
                    Icons.Outlined.CreateNewFolder,
                    contentDescription = stringResource(R.string.feature_items_move_to_group),
                )
            }
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Outlined.SelectAll, contentDescription = stringResource(R.string.feature_items_select_all))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}

/** Keeps the field, clear and close controls beside the keyboard and lower screen edge. */
@Composable
internal fun ItemsSearchBar(searchText: String, onSearchTextChange: (String) -> Unit, onClose: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Surface(tonalElevation = 3.dp) {
        TextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp).focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.feature_items_search_hint)) },
            leadingIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.feature_items_close_search),
                    )
                }
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChange("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.feature_items_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                keyboard?.hide()
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun SortMenu(sortOrder: ItemSortOrder, onSortOrderChange: (ItemSortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.feature_items_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ItemSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label()) },
                    leadingIcon = { RadioButton(selected = order == sortOrder, onClick = null) },
                    onClick = {
                        onSortOrderChange(order)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ItemSortOrder.label(): String = stringResource(
    when (this) {
        ItemSortOrder.EXPIRY_SOONEST -> R.string.feature_items_sort_expiry_soonest
        ItemSortOrder.EXPIRY_LATEST -> R.string.feature_items_sort_expiry_latest
        ItemSortOrder.NAME -> R.string.feature_items_sort_name
        ItemSortOrder.RECENTLY_ADDED -> R.string.feature_items_sort_recently_added
    },
)
