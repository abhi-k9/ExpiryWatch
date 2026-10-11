package io.github.abhik9.expirywatch.feature.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.domain.shopping.RestockReason
import io.github.abhik9.expirywatch.core.domain.shopping.RestockSuggestion
import io.github.abhik9.expirywatch.core.ui.expirySentence

/** How many suggestions show before the rest are folded away. */
private const val FOLDED_RESTOCK_COUNT = 3

/**
 * Products to buy again, because what the user had expired or is gone: a header with a button to
 * add them all, then the first few, with a button to show the rest.
 */
internal fun LazyListScope.restockSection(
    suggestions: List<RestockSuggestion>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAdd: (RestockSuggestion) -> Unit,
    onAddAll: () -> Unit,
    onDismiss: (RestockSuggestion) -> Unit,
) {
    if (suggestions.isEmpty()) return
    item(key = "restock", contentType = "restock-header") {
        Row(
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(
                text = stringResource(R.string.feature_shopping_restock, suggestions.size),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAddAll) { Text(stringResource(R.string.feature_shopping_add_all)) }
        }
    }
    val shown = if (expanded) suggestions else suggestions.take(FOLDED_RESTOCK_COUNT)
    items(shown, key = { "restock-${it.key}" }, contentType = { "restock" }) { suggestion ->
        RestockRow(
            suggestion = suggestion,
            onAdd = { onAdd(suggestion) },
            onDismiss = { onDismiss(suggestion) },
            modifier = Modifier.animateItem(),
        )
    }
    val hidden = suggestions.size - FOLDED_RESTOCK_COUNT
    if (hidden > 0) {
        item(key = "restock-more", contentType = "restock-more") {
            TextButton(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier
                    .animateItem()
                    .padding(start = 8.dp),
            ) {
                Text(
                    if (expanded) {
                        stringResource(R.string.feature_shopping_show_less)
                    } else {
                        pluralStringResource(R.plurals.feature_shopping_show_more, hidden, hidden)
                    },
                )
            }
        }
    }
}

@Composable
private fun RestockRow(
    suggestion: RestockSuggestion,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = listOfNotNull(suggestion.category?.emoji, suggestion.name).joinToString(" "),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = suggestion.reasonText(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (suggestion.reason == RestockReason.EXPIRED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = onAdd) {
            Icon(
                Icons.Outlined.AddShoppingCart,
                contentDescription = stringResource(R.string.feature_shopping_add_to_list, suggestion.name),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.feature_shopping_dismiss, suggestion.name),
            )
        }
    }
}

/** Why it's suggested, e.g. "Expired 2 days ago" or "Used up yesterday". */
@Composable
private fun RestockSuggestion.reasonText(): String {
    val days = daysAgo.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return when (reason) {
        RestockReason.EXPIRED -> expirySentence(-daysAgo)

        RestockReason.USED_UP -> when (days) {
            0 -> stringResource(R.string.feature_shopping_used_up_today)
            1 -> stringResource(R.string.feature_shopping_used_up_yesterday)
            else -> pluralStringResource(R.plurals.feature_shopping_used_up_days_ago, days, days)
        }

        RestockReason.THROWN_AWAY -> when (days) {
            0 -> stringResource(R.string.feature_shopping_thrown_away_today)
            1 -> stringResource(R.string.feature_shopping_thrown_away_yesterday)
            else -> pluralStringResource(R.plurals.feature_shopping_thrown_away_days_ago, days, days)
        }
    }
}
