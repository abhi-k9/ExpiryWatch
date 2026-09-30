package io.github.abhik9.expirywatch.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.scanner.BarcodeScannerDialog
import io.github.abhik9.expirywatch.core.ui.label
import java.time.LocalDate

@Composable
internal fun EditorRoute(
    openScanner: Boolean,
    onDone: () -> Unit,
    viewModel: EditorViewModel,
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Done -> onDone()
            }
        }
    }

    EditorScreen(
        isNewItem = viewModel.isNewItem,
        isLoading = viewModel.isLoading,
        isSaving = viewModel.isSaving,
        form = viewModel.form,
        errors = viewModel.errors,
        lookup = viewModel.lookup,
        categories = categories,
        locations = locations,
        today = viewModel.today,
        hasUnsavedChanges = viewModel.hasUnsavedChanges,
        openScannerInitially = openScanner,
        onFormChange = viewModel::updateForm,
        onBarcodeScanned = viewModel::onBarcodeScanned,
        onLookUpBarcode = viewModel::lookUpBarcode,
        onSave = viewModel::save,
        onFinish = viewModel::finish,
        onDelete = viewModel::delete,
        onClose = onDone,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(
    isNewItem: Boolean,
    isLoading: Boolean,
    isSaving: Boolean,
    form: EditorForm,
    errors: Set<EditorError>,
    lookup: LookupState,
    categories: List<Category>,
    locations: List<StorageLocation>,
    today: LocalDate,
    hasUnsavedChanges: Boolean,
    openScannerInitially: Boolean,
    onFormChange: ((EditorForm) -> EditorForm) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onLookUpBarcode: () -> Unit,
    onSave: () -> Unit,
    onFinish: (ItemStatus) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showScanner by rememberSaveable { mutableStateOf(openScannerInitially) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val barcodeFocusRequester = remember { FocusRequester() }

    val requestClose: () -> Unit = { if (hasUnsavedChanges) showDiscardDialog = true else onClose() }
    BackHandler(enabled = hasUnsavedChanges) { showDiscardDialog = true }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isNewItem) R.string.feature_editor_title_add else R.string.feature_editor_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_editor_close))
                    }
                },
                actions = {
                    Button(
                        onClick = onSave,
                        enabled = !isLoading && !isSaving,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(stringResource(R.string.feature_editor_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProductSection(
                form = form,
                lookup = lookup,
                barcodeFocusRequester = barcodeFocusRequester,
                onFormChange = onFormChange,
                onScanClick = { showScanner = true },
                onLookUpBarcode = onLookUpBarcode,
            )

            OutlinedTextField(
                value = form.name,
                onValueChange = { name -> onFormChange { it.copy(name = name) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.feature_editor_name)) },
                isError = EditorError.NAME_REQUIRED in errors,
                supportingText = if (EditorError.NAME_REQUIRED in errors) {
                    { Text(stringResource(R.string.feature_editor_error_name)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = form.brand,
                onValueChange = { brand -> onFormChange { it.copy(brand = brand) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.feature_editor_brand)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )

            SectionHeader(stringResource(R.string.feature_editor_section_expiry), Modifier.padding(top = 8.dp))
            DateField(
                label = stringResource(R.string.feature_editor_expiry_date),
                date = form.expiryDate,
                onDateChange = { date -> onFormChange { it.copy(expiryDate = date) } },
                isError = EditorError.EXPIRY_REQUIRED in errors,
                errorText = stringResource(R.string.feature_editor_error_expiry),
            )
            ExpiryQuickPicks(today = today, onPick = { date -> onFormChange { it.copy(expiryDate = date) } })

            OpenedSection(form = form, errors = errors, today = today, onFormChange = onFormChange)

            SectionHeader(stringResource(R.string.feature_editor_section_details), Modifier.padding(top = 8.dp))
            val selectedCategory = categories.firstOrNull { it.id == form.categoryId }
            val selectedLocation = locations.firstOrNull { it.id == form.locationId }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerField(
                    label = stringResource(R.string.feature_editor_category),
                    selectedText = selectedCategory?.let { "${it.emoji} ${it.name}" },
                    noneText = stringResource(R.string.feature_editor_none),
                    options = categories.map { it.id to "${it.emoji} ${it.name}" },
                    onSelect = { id -> onFormChange { it.copy(categoryId = id) } },
                    modifier = Modifier.weight(1f),
                )
                PickerField(
                    label = stringResource(R.string.feature_editor_location),
                    selectedText = selectedLocation?.let { "${it.emoji} ${it.name}" },
                    noneText = stringResource(R.string.feature_editor_none),
                    options = locations.map { it.id to "${it.emoji} ${it.name}" },
                    onSelect = { id -> onFormChange { it.copy(locationId = id) } },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.quantityText,
                    onValueChange = { text -> onFormChange { it.copy(quantityText = text) } },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.feature_editor_quantity)) },
                    isError = EditorError.QUANTITY_INVALID in errors,
                    supportingText = if (EditorError.QUANTITY_INVALID in errors) {
                        { Text(stringResource(R.string.feature_editor_error_quantity)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                )
                val unitLabels = QuantityUnit.entries.associateWith { it.label() }
                PickerField(
                    label = stringResource(R.string.feature_editor_unit),
                    selectedText = unitLabels.getValue(form.unit),
                    noneText = null,
                    options = QuantityUnit.entries.map { it.ordinal.toLong() to unitLabels.getValue(it) },
                    onSelect = { ordinal ->
                        val unit = QuantityUnit.entries[(ordinal ?: 0L).toInt()]
                        onFormChange { it.copy(unit = unit) }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = form.notes,
                onValueChange = { notes -> onFormChange { it.copy(notes = notes) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.feature_editor_notes)) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            if (!isNewItem) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ExistingItemActions(onFinish = onFinish, onDeleteClick = { showDeleteDialog = true })
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                showScanner = false
                onBarcodeScanned(barcode)
            },
            onEnterManually = {
                showScanner = false
                barcodeFocusRequester.requestFocus()
            },
            onDismiss = { showScanner = false },
        )
    }
    if (showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.feature_editor_discard_title),
            text = stringResource(R.string.feature_editor_discard_message),
            confirmText = stringResource(R.string.feature_editor_discard),
            onConfirm = {
                showDiscardDialog = false
                onClose()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }
    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.feature_editor_delete_title),
            text = stringResource(R.string.feature_editor_delete_message),
            confirmText = stringResource(R.string.feature_editor_delete),
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun OpenedSection(
    form: EditorForm,
    errors: Set<EditorError>,
    today: LocalDate,
    onFormChange: ((EditorForm) -> EditorForm) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.feature_editor_opened), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.feature_editor_opened_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = form.isOpened,
            onCheckedChange = { opened ->
                onFormChange { it.copy(isOpened = opened, openedDate = if (opened) it.openedDate ?: today else null) }
            },
        )
    }
    if (form.isOpened) {
        DateField(
            label = stringResource(R.string.feature_editor_opened_date),
            date = form.openedDate,
            onDateChange = { date -> onFormChange { it.copy(openedDate = date) } },
            latestSelectableDate = today,
        )
    }
    OutlinedTextField(
        value = form.useWithinDaysText,
        onValueChange = { text -> onFormChange { it.copy(useWithinDaysText = text.filter(Char::isDigit)) } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.feature_editor_use_within_days)) },
        isError = EditorError.OPENED_WINDOW_INVALID in errors,
        supportingText = {
            Text(
                stringResource(
                    if (EditorError.OPENED_WINDOW_INVALID in errors) {
                        R.string.feature_editor_error_use_within
                    } else {
                        R.string.feature_editor_use_within_days_help
                    },
                ),
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
}

@Composable
private fun ExistingItemActions(onFinish: (ItemStatus) -> Unit, onDeleteClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = { onFinish(ItemStatus.CONSUMED) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.feature_editor_mark_consumed), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = { onFinish(ItemStatus.WASTED) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.feature_editor_mark_wasted))
        }
    }
    TextButton(
        onClick = onDeleteClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.feature_editor_delete), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_editor_cancel)) } },
    )
}

@Composable
internal fun BarcodeField(
    barcode: String,
    onBarcodeChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onLookUp: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = barcode,
            onValueChange = onBarcodeChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            label = { Text(stringResource(R.string.feature_editor_barcode)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onLookUp() }),
            trailingIcon = {
                if (barcode.isNotBlank()) {
                    IconButton(onClick = onLookUp) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.feature_editor_look_up),
                        )
                    }
                }
            },
        )
        FilledTonalButton(onClick = onScanClick) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.feature_editor_scan), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
