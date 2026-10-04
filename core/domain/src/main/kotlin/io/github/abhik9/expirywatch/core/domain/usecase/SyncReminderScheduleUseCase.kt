package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ReminderScheduler
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.UserSettings
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Keeps the daily reminder in line with the user's settings. */
class SyncReminderScheduleUseCase @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    /**
     * Follows the settings for as long as the caller's scope is alive, so it's meant to be
     * launched once from an application-wide scope.
     */
    suspend operator fun invoke() {
        settingsRepository.settings
            .map { it.reminderSchedule() }
            .distinctUntilChanged()
            .collect { apply(it) }
    }

    /**
     * Applies the current settings once, for when the system dropped or moved the schedule
     * without them changing, such as after a restart.
     */
    suspend fun once() = apply(settingsRepository.settings.first().reminderSchedule())

    private suspend fun apply(schedule: Schedule?) {
        if (schedule == null) {
            reminderScheduler.cancel()
        } else {
            reminderScheduler.scheduleDaily(schedule.time, schedule.exact)
        }
    }

    private fun UserSettings.reminderSchedule(): Schedule? =
        if (remindersEnabled) Schedule(reminderTime, exactReminders) else null

    private data class Schedule(val time: LocalTime, val exact: Boolean)
}
