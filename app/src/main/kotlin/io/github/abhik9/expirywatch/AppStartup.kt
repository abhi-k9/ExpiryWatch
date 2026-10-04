package io.github.abhik9.expirywatch

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.AppInfo
import io.github.abhik9.expirywatch.core.common.di.ApplicationScope
import io.github.abhik9.expirywatch.core.diagnostics.DiagnosticsLog
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.usecase.SyncReminderScheduleUseCase
import io.github.abhik9.expirywatch.core.notifications.ExpiryNotifier
import io.github.abhik9.expirywatch.feature.widget.ExpiryWidgetUpdater
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Long-running work started with the process. */
class AppStartup @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val syncReminderSchedule: SyncReminderScheduleUseCase,
    private val itemRepository: ItemRepository,
    private val settingsRepository: UserSettingsRepository,
    private val widgetUpdater: ExpiryWidgetUpdater,
    private val notifier: ExpiryNotifier,
    private val diagnosticsLog: DiagnosticsLog,
    private val appInfo: AppInfo,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        diagnosticsLog.recordCrashes()
        diagnosticsLog.record { "app: process started, version ${appInfo.versionName}" }

        AppShortcuts.publish(context)
        notifier.createChannel()

        // Keep the daily reminder in line with the settings for as long as the process lives.
        applicationScope.launch { syncReminderSchedule() }

        // Redraw home-screen widgets whenever items or what counts as "expiring soon" change;
        // debounced to batch quick edits.
        applicationScope.launch {
            combine(
                itemRepository.observeActiveItems(),
                settingsRepository.settings.map { it.expiringSoonDays }.distinctUntilChanged(),
            ) { _, _ -> }
                .debounce(WIDGET_UPDATE_DEBOUNCE)
                .collect { widgetUpdater.updateAll() }
        }
    }

    private companion object {
        val WIDGET_UPDATE_DEBOUNCE = 1.seconds
    }
}
