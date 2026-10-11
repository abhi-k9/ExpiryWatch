package io.github.abhik9.expirywatch.feature.shopping.putaway

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.EmptyState
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.ui.DateField
import io.github.abhik9.expirywatch.core.ui.PickerField
import io.github.abhik9.expirywatch.core.ui.formatQuantity
import io.github.abhik9.expirywatch.feature.shopping.R
import java.time.LocalDate

@Composable
internal fun PutAwayRoute(
    onClose: () -> Unit,
    onShowItems: () -> Unit,
    viewModel: PutAwayViewModel,
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    PutAwayScreen(
        state = viewModel.state,
        locations = locations,
        onIncludedChange = viewModel::setIncluded,
        onExpiryDateChange = viewModel::setExpiryDate,
        onLocationChange = viewModel::setLocation,
        onSave = viewModel::save,
        onClose = onClose,
        onShowItems = onShowItems,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PutAwayScreen(
    state: PutAwayState,
    locations: List<StorageLocation>,
    onIncludedChange: (Long, Boolean) -> Unit,
    onExpiryDateChange: (Long, LocalDate) -> Unit,
    onLocationChange: (Long, StorageLocation?) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onShowItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_shopping_put_away)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_shopping_close))
                    }
                },
                actions = {
                    if (state is PutAwayState.Editing && state.rows.isNotEmpty()) {
                        Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.padding(end = 8.dp)) {
                            Text(stringResource(R.string.feature_shopping_put_away))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            PutAwayState.Loading, PutAwayState.Saving -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is PutAwayState.Editing -> if (state.rows.isEmpty()) {
                EmptyState(
                    emoji = "🛒",
                    title = stringResource(R.string.feature_shopping_put_away_empty_title),
                    message = stringResource(R.string.feature_shopping_put_away_empty_message),
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                PutAwayRows(
                    state = state,
                    locations = locations,
                    contentPadding = innerPadding,
                    onIncludedChange = onIncludedChange,
                    onExpiryDateChange = onExpiryDateChange,
                    onLocationChange = onLocationChange,
                )
            }

            is PutAwayState.Done -> EmptyState(
                emoji = "🧺",
                title = stringResource(R.string.feature_shopping_put_away_done_title),
                message = pluralStringResource(
                    R.plurals.feature_shopping_put_away_done_message,
                    state.count,
                    state.count,
                ),
                modifier = Modifier.padding(innerPadding),
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onShowItems,
                        ) { Text(stringResource(R.string.feature_shopping_show_items)) }
                        Button(onClick = onClose) { Text(stringResource(R.string.feature_shopping_done)) }
                    }
                },
            )
        }
    }
}

@Composable
private fun PutAwayRows(
    state: PutAwayState.Editing,
    locations: List<StorageLocation>,
    contentPadding: PaddingValues,
    onIncludedChange: (Long, Boolean) -> Unit,
    onExpiryDateChange: (Long, LocalDate) -> Unit,
    onLocationChange: (Long, StorageLocation?) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        item(key = "help", contentType = "help") {
            Text(
                text = stringResource(R.string.feature_shopping_put_away_help),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.rows, key = { it.id }, contentType = { "row" }) { row ->
            PutAwayRowItem(
                row = row,
                locations = locations,
                showError = state.showErrors && row.included && row.draft.expiryDate == null,
                onIncludedChange = { onIncludedChange(row.id, it) },
                onExpiryDateChange = { onExpiryDateChange(row.id, it) },
                onLocationChange = { onLocationChange(row.id, it) },
            )
        }
    }
}

@Composable
private fun PutAwayRowItem(
    row: PutAwayRow,
    locations: List<StorageLocation>,
    showError: Boolean,
    onIncludedChange: (Boolean) -> Unit,
    onExpiryDateChange: (LocalDate) -> Unit,
    onLocationChange: (StorageLocation?) -> Unit,
) {
    val bought = row.draft.bought
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = row.included, role = Role.Checkbox, onValueChange = onIncludedChange)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Checkbox(checked = row.included, onCheckedChange = null)
            Text(
                text = listOfNotNull(
                    bought.name,
                    formatQuantity(bought.quantity, bought.unit).takeIf {
                        bought.quantity != 1.0 || bought.unit != QuantityUnit.PIECES
                    },
                ).joinToString(" · "),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (row.included) {
            Row(
                modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DateField(
                    label = stringResource(R.string.feature_shopping_expires),
                    date = row.draft.expiryDate,
                    onDateChange = onExpiryDateChange,
                    modifier = Modifier.weight(1f),
                    isError = showError,
                    errorText = stringResource(R.string.feature_shopping_expiry_required),
                )
                PickerField(
                    label = stringResource(R.string.feature_shopping_location),
                    selectedText = row.draft.location?.let { "${it.emoji} ${it.name}" },
                    noneText = stringResource(R.string.feature_shopping_none),
                    options = locations.map { it.id to "${it.emoji} ${it.name}" },
                    onSelect = { id -> onLocationChange(locations.firstOrNull { it.id == id }) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
