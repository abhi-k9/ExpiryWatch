package io.github.abhik9.expirywatch.feature.settings

import android.Manifest
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.expirywatch.core.common.AppInfo
import io.github.abhik9.expirywatch.core.designsystem.component.SectionHeader
import io.github.abhik9.expirywatch.core.designsystem.theme.supportsDynamicTheming
import io.github.abhik9.expirywatch.core.domain.backup.BackupCodec
import io.github.abhik9.expirywatch.core.domain.usecase.BackupResult
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.model.UserSettings
import io.github.abhik9.expirywatch.core.navigation.LabelKind
import io.github.abhik9.expirywatch.core.ui.formatShort
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
internal fun SettingsRoute(
    onManageLabels: (LabelKind) -> Unit,
    onOpenAdvanced: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Updated on configuration changes without restarting the effect, which would drop a shown snackbar.
    val resources by rememberUpdatedState(LocalResources.current)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is SettingsEvent.BackupFinished -> resources.backupMessage(event.operation, event.result)

                is SettingsEvent.DiagnosticsExported -> resources.getString(
                    if (event.success) {
                        R.string.feature_settings_diagnostics_exported
                    } else {
                        R.string.feature_settings_diagnostics_export_failed
                    },
                )
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupCodec.MIME_TYPE),
    ) { uri -> uri?.let { viewModel.exportTo(it.toString()) } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFrom(it.toString()) }
    }
    val diagnosticsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(DIAGNOSTICS_MIME_TYPE),
    ) { uri -> uri?.let { viewModel.exportDiagnosticsTo(it.toString()) } }

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
        onExactRemindersChange = viewModel::setExactReminders,
        onManageLabels = onManageLabels,
        onOpenAdvanced = onOpenAdvanced,
        onExport = { exportLauncher.launch(viewModel.suggestedBackupFileName) },
        // Some file managers label JSON files as plain text or a generic binary.
        onImport = { importLauncher.launch(arrayOf(BackupCodec.MIME_TYPE, "text/plain", "application/octet-stream")) },
        diagnostics = diagnostics,
        onDiagnosticsRecordingChange = viewModel::setDiagnosticsRecording,
        onExportDiagnostics = { diagnosticsExportLauncher.launch(viewModel.suggestedDiagnosticsFileName) },
        onClearDiagnostics = viewModel::clearDiagnostics,
    )
}

private fun Resources.backupMessage(operation: BackupOperation, result: BackupResult): String = when (result) {
    is BackupResult.Success -> getQuantityString(
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
    onExactRemindersChange: (Boolean) -> Unit,
    onManageLabels: (LabelKind) -> Unit,
    onOpenAdvanced: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    diagnostics: DiagnosticsState,
    onDiagnosticsRecordingChange: (Boolean) -> Unit,
    onExportDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
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
            RemindersSection(
                settings = settings,
                onExpiringSoonDaysChange = onExpiringSoonDaysChange,
                onRemindersEnabledChange = onRemindersEnabledChange,
                onReminderTimeChange = onReminderTimeChange,
                onExactRemindersChange = onExactRemindersChange,
            )

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
            DiagnosticsSection(diagnostics, onDiagnosticsRecordingChange, onExportDiagnostics, onClearDiagnostics)

            SectionHeader(stringResource(R.string.feature_settings_section_advanced))
            SettingsRow(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.feature_settings_advanced),
                subtitle = stringResource(R.string.feature_settings_advanced_summary),
                onClick = onOpenAdvanced,
            )

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
    onExactRemindersChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    // Checked again on return to the screen, since the user may change them in the system settings.
    var canPostNotifications by remember { mutableStateOf(context.canPostNotifications()) }
    var canScheduleExactAlarms by remember { mutableStateOf(context.canScheduleExactAlarms()) }
    LifecycleResumeEffect(context) {
        canPostNotifications = context.canPostNotifications()
        canScheduleExactAlarms = context.canScheduleExactAlarms()
        onPauseOrDispose {}
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canPostNotifications = context.canPostNotifications()
    }

    SectionHeader(stringResource(R.string.feature_settings_section_reminders))
    SwitchRow(
        icon = Icons.Outlined.Notifications,
        title = stringResource(R.string.feature_settings_reminders),
        subtitle = stringResource(R.string.feature_settings_reminders_summary),
        checked = settings.remindersEnabled,
        onCheckedChange = { enabled ->
            onRemindersEnabledChange(enabled)
            if (enabled && !canPostNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )
    // If the permission was denied for good, or notifications were turned off in the system
    // settings, the app can't ask again: point to where the user can allow them.
    if (settings.remindersEnabled && !canPostNotifications) {
        SettingsRow(
            icon = Icons.Outlined.NotificationsOff,
            title = stringResource(R.string.feature_settings_notifications_blocked),
            subtitle = stringResource(R.string.feature_settings_notifications_blocked_summary),
            onClick = { context.openNotificationSettings() },
        )
    }
    SettingsRow(
        icon = Icons.Outlined.Schedule,
        title = stringResource(R.string.feature_settings_reminder_time),
        subtitle = settings.reminderTime.formatShort(),
        enabled = settings.remindersEnabled,
        onClick = { showTimePicker = true },
    )
    SwitchRow(
        icon = Icons.Outlined.Alarm,
        title = stringResource(R.string.feature_settings_exact_reminders),
        subtitle = stringResource(R.string.feature_settings_exact_reminders_summary),
        checked = settings.exactReminders,
        enabled = settings.remindersEnabled,
        onCheckedChange = { exact ->
            onExactRemindersChange(exact)
            if (exact && !canScheduleExactAlarms) context.openExactAlarmSettings()
        },
    )
    // Since Android 12, only the user can allow the app to set alarms, in the system settings.
    if (settings.remindersEnabled && settings.exactReminders && !canScheduleExactAlarms) {
        SettingsRow(
            icon = Icons.Outlined.AlarmOff,
            title = stringResource(R.string.feature_settings_alarms_blocked),
            subtitle = stringResource(R.string.feature_settings_alarms_blocked_summary),
            onClick = { context.openExactAlarmSettings() },
        )
    }

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
private fun DiagnosticsSection(
    diagnostics: DiagnosticsState,
    onRecordingChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    SectionHeader(stringResource(R.string.feature_settings_section_diagnostics))
    SwitchRow(
        icon = Icons.Outlined.BugReport,
        title = stringResource(R.string.feature_settings_diagnostics),
        subtitle = stringResource(R.string.feature_settings_diagnostics_summary),
        checked = diagnostics.isRecording,
        onCheckedChange = onRecordingChange,
    )
    // Only once something has been recorded.
    if (diagnostics.logBytes > 0) {
        val size = Formatter.formatShortFileSize(LocalContext.current, diagnostics.logBytes)
        SettingsRow(
            icon = Icons.Outlined.Description,
            title = stringResource(R.string.feature_settings_diagnostics_export),
            subtitle = stringResource(R.string.feature_settings_diagnostics_export_summary, size),
            onClick = onExport,
        )
        SettingsRow(
            icon = Icons.Outlined.DeleteSweep,
            title = stringResource(R.string.feature_settings_diagnostics_clear),
            onClick = onClear,
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
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)) }
        } else {
            null
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
internal fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        modifier = Modifier.toggleableRow(checked, onCheckedChange, enabled),
    )
}

// Covers both the Android 13+ permission and notifications turned off in system settings.
private fun Context.canPostNotifications(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

private fun Context.openNotificationSettings() {
    val notificationSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    try {
        startActivity(notificationSettings)
    } catch (e: ActivityNotFoundException) {
        // Some devices don't have the per-app notification screen; the app's details page does.
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }
}

// Always allowed before Android 12; since then, the user allows it under "Alarms & reminders".
private fun Context.canScheduleExactAlarms(): Boolean =
    AlarmManagerCompat.canScheduleExactAlarms(checkNotNull(getSystemService(AlarmManager::class.java)))

private fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val packageUri = Uri.fromParts("package", packageName, null)
    try {
        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
    }
}

private const val DISABLED_ALPHA = 0.38f
private const val DIAGNOSTICS_MIME_TYPE = "text/plain"
private const val OPEN_FOOD_FACTS_URL = "https://world.openfoodfacts.org"
