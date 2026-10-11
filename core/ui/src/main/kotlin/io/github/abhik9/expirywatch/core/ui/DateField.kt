package io.github.abhik9.expirywatch.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A read-only field that opens a date picker when tapped. */
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    latestSelectableDate: LocalDate? = null,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = date?.formatMedium().orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null) },
            isError = isError,
            supportingText = if (isError && errorText != null) {
                { Text(errorText) }
            } else {
                null
            },
            singleLine = true,
        )
        // The read-only text field would swallow taps, so an invisible layer on top opens the picker.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = label) { showPicker = true },
        )
    }

    if (showPicker) {
        PickDateDialog(
            initialDate = date,
            latestSelectableDate = latestSelectableDate,
            onConfirm = { picked ->
                onDateChange(picked)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

/** Asks for a date, starting at [initialDate], up to [latestSelectableDate] when there is one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDateDialog(
    initialDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    latestSelectableDate: LocalDate? = null,
) {
    val latestMillis = latestSelectableDate?.toUtcMillis()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                latestMillis == null || utcTimeMillis <= latestMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { pickerState.selectedDateMillis?.let { onConfirm(it.utcMillisToLocalDate()) } },
                enabled = pickerState.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.core_ui_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_ui_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

// The Material date picker works in UTC milliseconds at midnight.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
