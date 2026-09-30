package io.github.abhik9.expirywatch

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.di.ApplicationScope
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.usecase.SyncReminderScheduleUseCase
import io.github.abhik9.expirywatch.feature.widget.ExpiryWidgetUpdater
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/** Long-running work started with the process. */
class AppStartup @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val syncReminderSchedule: SyncReminderScheduleUseCase,
    private val itemRepository: ItemRepository,
    private val widgetUpdater: ExpiryWidgetUpdater,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        AppShortcuts.publish(context)

        // Keep the daily reminder in line with the settings for as long as the process lives.
        applicationScope.launch { syncReminderSchedule() }

        // Redraw home-screen widgets whenever items change; debounced to batch quick edits.
        applicationScope.launch {
            itemRepository.observeActiveItems()
                .debounce(WIDGET_UPDATE_DEBOUNCE)
                .collect { widgetUpdater.updateAll() }
        }
    }

    private companion object {
        val WIDGET_UPDATE_DEBOUNCE = 1.seconds
    }
}
