package io.github.abhik9.expirywatch.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.common.AppInfo
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.usecase.BackupResult
import io.github.abhik9.expirywatch.core.domain.usecase.ExportBackupUseCase
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data class BackupFinished(val operation: BackupOperation, val result: BackupResult) : SettingsEvent
}

enum class BackupOperation {
    EXPORT,
    IMPORT,
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    private val clock: Clock,
    val appInfo: AppInfo,
) : ViewModel() {
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    var isBackupInProgress by mutableStateOf(false)
        private set

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    /** A dated default name for the export file, e.g. "expirywatch-backup-2026-03-10.json". */
    val suggestedBackupFileName: String get() = "expirywatch-backup-${LocalDate.now(clock)}.json"

    fun setThemeMode(themeMode: ThemeMode) = launch { settingsRepository.setThemeMode(themeMode) }

    fun setUseDynamicColor(enabled: Boolean) = launch { settingsRepository.setUseDynamicColor(enabled) }

    fun setExpiringSoonDays(days: Int) = launch { settingsRepository.setExpiringSoonDays(days) }

    fun setRemindersEnabled(enabled: Boolean) = launch { settingsRepository.setRemindersEnabled(enabled) }

    fun setReminderTime(time: LocalTime) = launch { settingsRepository.setReminderTime(time) }

    fun exportTo(uri: String) = runBackup(BackupOperation.EXPORT) { exportBackup(uri) }

    fun importFrom(uri: String) = runBackup(BackupOperation.IMPORT) { importBackup(uri) }

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
