package io.github.abhik9.expirywatch.feature.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.common.AppInfo
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.designsystem.theme.supportsDynamicTheming
import io.github.abhik9.expirywatch.core.domain.backup.BackupCodec
import io.github.abhik9.expirywatch.core.domain.usecase.BackupResult
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.model.UserSettings
import io.github.abhik9.expirywatch.core.navigation.LabelKind
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
internal fun SettingsRoute(
    onManageLabels: (LabelKind) -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.BackupFinished ->
                    snackbarHostState.showSnackbar(context.backupMessage(event.operation, event.result))
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupCodec.MIME_TYPE),
    ) { uri -> uri?.let { viewModel.exportTo(it.toString()) } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFrom(it.toString()) }
    }

    SettingsScreen(
        settings = settings,
        appInfo = viewModel.appInfo,
        isBackupInProgress = viewModel.isBackupInProgress,
        snackbarHostState = snackbarHostState,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setUseDynamicColor,
        onExpiringSoonDaysChange = viewModel::setExpiringSoonDays,
        onRemindersEnabledChange = viewModel::setRemindersEnabled,
        onReminderTimeChange = viewModel::setReminderTime,
        onManageLabels = onManageLabels,
        onExport = { exportLauncher.launch(viewModel.suggestedBackupFileName) },
        // Some file managers label JSON files as plain text or a generic binary.
        onImport = { importLauncher.launch(arrayOf(BackupCodec.MIME_TYPE, "text/plain", "application/octet-stream")) },
    )
}

private fun Context.backupMessage(operation: BackupOperation, result: BackupResult): String = when (result) {
    is BackupResult.Success -> resources.getQuantityString(
        when (operation) {
            BackupOperation.EXPORT -> R.plurals.feature_settings_export_done
            BackupOperation.IMPORT -> R.plurals.feature_settings_import_done
        },
        result.stats.itemCount,
        result.stats.itemCount,
    )

    BackupResult.IoError -> getString(R.string.feature_settings_backup_io_error)

    is BackupResult.InvalidFile -> getString(R.string.feature_settings_backup_invalid_file)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    settings: UserSettings?,
    appInfo: AppInfo,
    isBackupInProgress: Boolean,
    snackbarHostState: SnackbarHostState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExpiringSoonDaysChange: (Int) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onManageLabels: (LabelKind) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (settings == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            AppearanceSection(settings, onThemeModeChange, onDynamicColorChange)
            RemindersSection(settings, onExpiringSoonDaysChange, onRemindersEnabledChange, onReminderTimeChange)

            SectionHeader(stringResource(R.string.feature_settings_section_organize))
            SettingsRow(
                icon = Icons.Outlined.Category,
                title = stringResource(R.string.feature_settings_categories),
                subtitle = stringResource(R.string.feature_settings_categories_summary),
                onClick = { onManageLabels(LabelKind.CATEGORIES) },
            )
            SettingsRow(
                icon = Icons.Outlined.Kitchen,
                title = stringResource(R.string.feature_settings_locations),
                subtitle = stringResource(R.string.feature_settings_locations_summary),
                onClick = { onManageLabels(LabelKind.LOCATIONS) },
            )

            BackupSection(isBackupInProgress = isBackupInProgress, onExport = onExport, onImport = onImport)
            AboutSection(appInfo)
        }
    }
}

@Composable
private fun AppearanceSection(
    settings: UserSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    SectionHeader(stringResource(R.string.feature_settings_section_appearance))
    ListItem(
        headlineContent = { Text(stringResource(R.string.feature_settings_theme)) },
        leadingContent = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
        supportingContent = {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        label = { Text(mode.label()) },
                    )
                }
            }
        },
    )
    if (supportsDynamicTheming()) {
        SwitchRow(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.feature_settings_dynamic_color),
            subtitle = stringResource(R.string.feature_settings_dynamic_color_summary),
            checked = settings.useDynamicColor,
            onCheckedChange = onDynamicColorChange,
        )
    }
}

@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.feature_settings_theme_system
        ThemeMode.LIGHT -> R.string.feature_settings_theme_light
        ThemeMode.DARK -> R.string.feature_settings_theme_dark
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindersSection(
    settings: UserSettings,
    onExpiringSoonDaysChange: (Int) -> Unit,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onRemindersEnabledChange(granted)
    }

    SectionHeader(stringResource(R.string.feature_settings_section_reminders))
    SwitchRow(
        icon = Icons.Outlined.Notifications,
        title = stringResource(R.string.feature_settings_reminders),
        subtitle = stringResource(R.string.feature_settings_reminders_summary),
        checked = settings.remindersEnabled && context.canPostNotifications(),
        onCheckedChange = { enabled ->
            if (enabled && !context.canPostNotifications() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onRemindersEnabledChange(enabled)
            }
        },
    )
    SettingsRow(
        icon = Icons.Outlined.Schedule,
        title = stringResource(R.string.feature_settings_reminder_time),
        subtitle = settings.reminderTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
        enabled = settings.remindersEnabled,
        onClick = { showTimePicker = true },
    )

    // The slider moves freely while dragging; the setting is saved when the finger lifts.
    var sliderDays by remember(settings.expiringSoonDays) { mutableFloatStateOf(settings.expiringSoonDays.toFloat()) }
    val range = UserSettings.EXPIRING_SOON_DAYS_RANGE
    ListItem(
        headlineContent = { Text(stringResource(R.string.feature_settings_expiring_soon)) },
        leadingContent = { Icon(Icons.Outlined.Timelapse, contentDescription = null) },
        supportingContent = {
            Column {
                Text(
                    pluralStringResource(
                        R.plurals.feature_settings_expiring_soon_summary,
                        sliderDays.roundToInt(),
                        sliderDays.roundToInt(),
                    ),
                )
                Slider(
                    value = sliderDays,
                    onValueChange = { sliderDays = it },
                    onValueChangeFinished = { onExpiringSoonDaysChange(sliderDays.roundToInt()) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = range.last - range.first - 1,
                )
            }
        },
    )

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = settings.reminderTime.hour,
            initialMinute = settings.reminderTime.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.feature_settings_reminder_time)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReminderTimeChange(LocalTime.of(state.hour, state.minute))
                        showTimePicker = false
                    },
                ) { Text(stringResource(R.string.feature_settings_ok)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false },
                ) { Text(stringResource(R.string.feature_settings_cancel)) }
            },
        )
    }
}

@Composable
private fun BackupSection(isBackupInProgress: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    var confirmImport by rememberSaveable { mutableStateOf(false) }

    SectionHeader(stringResource(R.string.feature_settings_section_backup))
    if (isBackupInProgress) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
    SettingsRow(
        icon = Icons.Outlined.FileUpload,
        title = stringResource(R.string.feature_settings_export),
        subtitle = stringResource(R.string.feature_settings_export_summary),
        enabled = !isBackupInProgress,
        onClick = onExport,
    )
    SettingsRow(
        icon = Icons.Outlined.FileDownload,
        title = stringResource(R.string.feature_settings_import),
        subtitle = stringResource(R.string.feature_settings_import_summary),
        enabled = !isBackupInProgress,
        onClick = { confirmImport = true },
    )

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.feature_settings_import_confirm_title)) },
            text = { Text(stringResource(R.string.feature_settings_import_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmImport = false
                        onImport()
                    },
                ) { Text(stringResource(R.string.feature_settings_import_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmImport = false },
                ) { Text(stringResource(R.string.feature_settings_cancel)) }
            },
        )
    }
}

@Composable
private fun AboutSection(appInfo: AppInfo) {
    val uriHandler = LocalUriHandler.current
    SectionHeader(stringResource(R.string.feature_settings_section_about))
    SettingsRow(
        icon = Icons.Outlined.Info,
        title = stringResource(R.string.feature_settings_version),
        subtitle = appInfo.versionName,
    )
    SettingsRow(
        icon = Icons.Outlined.QrCodeScanner,
        title = stringResource(R.string.feature_settings_barcode_engine),
        subtitle = appInfo.barcodeEngine,
    )
    SettingsRow(
        icon = Icons.Outlined.Language,
        title = stringResource(R.string.feature_settings_open_food_facts),
        subtitle = stringResource(R.string.feature_settings_open_food_facts_summary),
        onClick = { uriHandler.openUri(OPEN_FOOD_FACTS_URL) },
    )
    SettingsRow(
        icon = Icons.Outlined.Code,
        title = stringResource(R.string.feature_settings_source_code),
        subtitle = appInfo.sourceCodeUrl,
        onClick = { uriHandler.openUri(appInfo.sourceCodeUrl) },
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)) },
        supportingContent = subtitle?.let {
            { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)) }
        },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = if (onClick != null && enabled) {
            Modifier.clickableRow(onClick)
        } else {
            Modifier
        },
    )
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleableRow(checked, onCheckedChange),
    )
}

private fun Context.canPostNotifications(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private const val DISABLED_ALPHA = 0.38f
private const val OPEN_FOOD_FACTS_URL = "https://world.openfoodfacts.org"
