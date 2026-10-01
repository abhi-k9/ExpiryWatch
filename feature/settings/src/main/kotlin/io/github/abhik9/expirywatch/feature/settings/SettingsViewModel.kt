package io.github.abhik9.expirywatch.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.common.AppInfo
import io.github.abhik9.expirywatch.core.domain.repository.DiagnosticsRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.usecase.BackupResult
import io.github.abhik9.expirywatch.core.domain.usecase.ExportBackupUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ExportDiagnosticsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ImportBackupUseCase
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.model.UserSettings
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data class BackupFinished(val operation: BackupOperation, val result: BackupResult) : SettingsEvent

    data class DiagnosticsExported(val success: Boolean) : SettingsEvent
}

data class DiagnosticsState(
    val isRecording: Boolean,
    /** Size of the recorded log, in bytes. */
    val logBytes: Long,
)

enum class BackupOperation {
    EXPORT,
    IMPORT,
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val exportDiagnostics: ExportDiagnosticsUseCase,
    private val clock: Clock,
    val appInfo: AppInfo,
) : ViewModel() {
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val diagnostics: StateFlow<DiagnosticsState> =
        combine(diagnosticsRepository.isRecording, diagnosticsRepository.logSize, ::DiagnosticsState).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            DiagnosticsState(diagnosticsRepository.isRecording.value, diagnosticsRepository.logSize.value),
        )

    var isBackupInProgress by mutableStateOf(false)
        private set

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    /** A dated default name for the export file, e.g. "expirywatch-backup-2026-03-10.json". */
    val suggestedBackupFileName: String get() = "expirywatch-backup-${LocalDate.now(clock)}.json"

    /** A dated default name for the diagnostics file, e.g. "expirywatch-diagnostics-2026-03-10.txt". */
    val suggestedDiagnosticsFileName: String get() = "expirywatch-diagnostics-${LocalDate.now(clock)}.txt"

    fun setThemeMode(themeMode: ThemeMode) = launch { settingsRepository.setThemeMode(themeMode) }

    fun setUseDynamicColor(enabled: Boolean) = launch { settingsRepository.setUseDynamicColor(enabled) }

    fun setExpiringSoonDays(days: Int) = launch { settingsRepository.setExpiringSoonDays(days) }

    fun setRemindersEnabled(enabled: Boolean) = launch { settingsRepository.setRemindersEnabled(enabled) }

    fun setReminderTime(time: LocalTime) = launch { settingsRepository.setReminderTime(time) }

    fun exportTo(uri: String) = runBackup(BackupOperation.EXPORT) { exportBackup(uri) }

    fun importFrom(uri: String) = runBackup(BackupOperation.IMPORT) { importBackup(uri) }

    fun setDiagnosticsRecording(enabled: Boolean) = diagnosticsRepository.setRecording(enabled)

    fun clearDiagnostics() = launch { diagnosticsRepository.clear() }

    fun exportDiagnosticsTo(uri: String) = launch {
        _events.send(SettingsEvent.DiagnosticsExported(success = exportDiagnostics(uri)))
    }

    private fun runBackup(operation: BackupOperation, block: suspend () -> BackupResult) {
        if (isBackupInProgress) return
        isBackupInProgress = true
        viewModelScope.launch {
            val result = block()
            isBackupInProgress = false
            _events.send(SettingsEvent.BackupFinished(operation, result))
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
