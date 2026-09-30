package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ReminderScheduler
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Keeps the daily reminder in line with the user's settings. Suspends for as long as the caller's
 * scope is alive, so it's meant to be launched once from an application-wide scope.
 */
class SyncReminderScheduleUseCase @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke() {
        settingsRepository.settings
            .map { settings -> settings.reminderTime.takeIf { settings.remindersEnabled } }
            .distinctUntilChanged()
            .collect { time ->
                if (time == null) reminderScheduler.cancel() else reminderScheduler.scheduleDaily(time)
            }
    }
}
