package io.github.abhik9.expirywatch.feature.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.ui.DateField
import io.github.abhik9.expirywatch.core.ui.PickDateDialog
import io.github.abhik9.expirywatch.core.ui.formatMedium
import java.time.LocalDate

@Composable
internal fun ProductSection(
    form: EditorForm,
    lookup: LookupState,
    barcodeFocusRequester: FocusRequester,
    onFormChange: ((EditorForm) -> EditorForm) -> Unit,
    onScanClick: () -> Unit,
    onLookUpBarcode: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (form.imageUrl != null) {
            AsyncImage(
                model = form.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        BarcodeField(
            barcode = form.barcode,
            onBarcodeChange = { barcode -> onFormChange { it.copy(barcode = barcode) } },
            onScanClick = onScanClick,
            onLookUp = onLookUpBarcode,
            focusRequester = barcodeFocusRequester,
            modifier = Modifier.weight(1f),
        )
    }
    LookupStatus(lookup)
}

@Composable
private fun LookupStatus(lookup: LookupState) {
    val message = when (lookup) {
        LookupState.Idle -> return

        LookupState.Loading -> stringResource(R.string.feature_editor_lookup_loading)

        is LookupState.Found -> stringResource(
            when (lookup.source) {
                ProductSource.HISTORY -> R.string.feature_editor_lookup_found_history
                ProductSource.OPEN_FOOD_FACTS -> R.string.feature_editor_lookup_found_off
            },
        )

        LookupState.NotFound -> stringResource(R.string.feature_editor_lookup_not_found)

        LookupState.Unavailable -> stringResource(R.string.feature_editor_lookup_unavailable)

        LookupState.NotFoundOnlineLookupOff -> stringResource(R.string.feature_editor_lookup_online_off)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (lookup == LookupState.Loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (lookup == LookupState.Unavailable) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * More of the same product that expires on other days, such as the rest of a multipack. Each date
 * is saved as an unopened item of its own, with the same name and details.
 *
 * @param suggestedDate where the date picker starts when adding a date.
 */
@Composable
internal fun AdditionalDates(
    dates: List<AdditionalDateForm>,
    invalidQuantities: Set<Int>,
    suggestedDate: LocalDate?,
    onAdd: (LocalDate) -> Unit,
    onChange: (key: Int, transform: (AdditionalDateForm) -> AdditionalDateForm) -> Unit,
    onRemove: (key: Int) -> Unit,
) {
    dates.forEach { row ->
        key(row.key) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(
                    label = stringResource(R.string.feature_editor_expiry_date),
                    date = row.expiryDate,
                    onDateChange = { date -> onChange(row.key) { it.copy(expiryDate = date) } },
                    modifier = Modifier.weight(3f),
                )
                val isInvalid = row.key in invalidQuantities
                OutlinedTextField(
                    value = row.quantityText,
                    onValueChange = { text -> onChange(row.key) { it.copy(quantityText = text) } },
                    modifier = Modifier.weight(2f),
                    label = { Text(stringResource(R.string.feature_editor_quantity)) },
                    isError = isInvalid,
                    supportingText = if (isInvalid) {
                        { Text(stringResource(R.string.feature_editor_error_quantity)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                IconButton(onClick = { onRemove(row.key) }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Outlined.RemoveCircleOutline,
                        contentDescription = stringResource(
                            R.string.feature_editor_remove_date,
                            row.expiryDate.formatMedium(),
                        ),
                    )
                }
            }
        }
    }

    var showPicker by rememberSaveable { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { showPicker = true }) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.feature_editor_add_date))
        }
        Text(
            text = stringResource(R.string.feature_editor_add_date_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    if (showPicker) {
        PickDateDialog(
            initialDate = suggestedDate,
            onConfirm = { date ->
                onAdd(date)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** One-tap shortcuts for common shelf lives, relative to today. */
@Composable
internal fun ExpiryQuickPicks(today: LocalDate, onPick: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickPick.entries.forEach { pick ->
            SuggestionChip(
                onClick = { onPick(pick.from(today)) },
                label = { Text(pick.label()) },
            )
        }
    }
}

private enum class QuickPick(val days: Long = 0, val weeks: Long = 0, val months: Long = 0) {
    THREE_DAYS(days = 3),
    ONE_WEEK(weeks = 1),
    TWO_WEEKS(weeks = 2),
    ONE_MONTH(months = 1),
    THREE_MONTHS(months = 3),
    SIX_MONTHS(months = 6),
    ONE_YEAR(months = 12),
    ;

    fun from(today: LocalDate): LocalDate = today.plusDays(days).plusWeeks(weeks).plusMonths(months)

    @Composable
    fun label(): String = when {
        months == 12L -> stringResource(R.string.feature_editor_quick_one_year)
        months > 0 -> pluralStringResource(R.plurals.feature_editor_quick_months, months.toInt(), months.toInt())
        weeks > 0 -> pluralStringResource(R.plurals.feature_editor_quick_weeks, weeks.toInt(), weeks.toInt())
        else -> pluralStringResource(R.plurals.feature_editor_quick_days, days.toInt(), days.toInt())
    }
}
