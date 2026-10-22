package io.github.abhik9.expirywatch.feature.items

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.designsystem.theme.ExpiryWatchTheme
import io.github.abhik9.expirywatch.core.domain.usecase.ItemQuery
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.ui.colors
import io.github.abhik9.expirywatch.core.ui.label

/** Three tappable tiles counting expired, soon-to-expire and fresh items; tapping filters. */
@Composable
internal fun StatusSummaryRow(
    counts: Map<ExpiryStatus, Int>,
    selectedStatus: ExpiryStatus?,
    onStatusSelected: (ExpiryStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpiryStatus.entries.forEach { status ->
            StatusTile(
                status = status,
                count = counts[status] ?: 0,
                selected = selectedStatus == status,
                onClick = { onStatusSelected(if (selectedStatus == status) null else status) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusTile(
    status: ExpiryStatus,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = status.colors
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (selected) colors.container else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) colors.onContainer else MaterialTheme.colorScheme.onSurface,
        border = if (selected) BorderStroke(2.dp, colors.accent) else null,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Text(
                    text = status.label(),
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun FilterRow(
    query: ItemQuery,
    categories: List<Category>,
    locations: List<StorageLocation>,
    groups: List<CustomGroup>,
    onCategoryFilterChange: (Long?) -> Unit,
    onLocationFilterChange: (Long?) -> Unit,
    onGroupFilterChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val selectedCategory = categories.firstOrNull { it.id == query.categoryId }
        DropdownFilterChip(
            label = selectedCategory?.let { "${it.emoji} ${it.name}" }
                ?: stringResource(R.string.feature_items_filter_all_categories),
            selected = selectedCategory != null,
            anyLabel = stringResource(R.string.feature_items_filter_all_categories),
            options = categories.map { it.id to "${it.emoji} ${it.name}" },
            onSelect = onCategoryFilterChange,
        )
        val selectedLocation = locations.firstOrNull { it.id == query.locationId }
        DropdownFilterChip(
            label = selectedLocation?.let { "${it.emoji} ${it.name}" }
                ?: stringResource(R.string.feature_items_filter_all_locations),
            selected = selectedLocation != null,
            anyLabel = stringResource(R.string.feature_items_filter_all_locations),
            options = locations.map { it.id to "${it.emoji} ${it.name}" },
            onSelect = onLocationFilterChange,
        )
        // Only once the user has groups, or one is still picked.
        val selectedGroup = groups.firstOrNull { it.id == query.groupId }
        if (groups.isNotEmpty() || query.groupId != null) {
            DropdownFilterChip(
                label = selectedGroup?.let { "${it.emoji} ${it.name}" }
                    ?: stringResource(R.string.feature_items_filter_all_groups),
                selected = query.groupId != null,
                anyLabel = stringResource(R.string.feature_items_filter_all_groups),
                options = groups.sortedBy { it.name.lowercase() }.map { it.id to "${it.emoji} ${it.name}" },
                onSelect = onGroupFilterChange,
            )
        }
        if (query.hasFilters) {
            TextButton(onClick = onClearFilters) {
                Text(stringResource(R.string.feature_items_clear_filters))
            }
        }
    }
}

@Composable
private fun DropdownFilterChip(
    label: String,
    selected: Boolean,
    anyLabel: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(anyLabel) },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun StatusSummaryRowPreview() {
    ExpiryWatchTheme(dynamicColor = false) {
        StatusSummaryRow(
            counts = mapOf(ExpiryStatus.EXPIRED to 1, ExpiryStatus.EXPIRING_SOON to 4, ExpiryStatus.FRESH to 23),
            selectedStatus = ExpiryStatus.EXPIRING_SOON,
            onStatusSelected = {},
        )
    }
}
