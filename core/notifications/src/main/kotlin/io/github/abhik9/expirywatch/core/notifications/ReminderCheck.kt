package io.github.abhik9.expirywatch.core.notifications

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.usecase.GetExpirySummaryUseCase
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Notifies the user about expired and soon-to-expire items, then sets the next check to the
 * reminder time, in case this one came late. Run by the daily work and, for exact reminders, the
 * alarm; whichever comes first shows the day's reminder.
 */
internal class ReminderCheck @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val getExpirySummary: GetExpirySummaryUseCase,
    private val notifier: ExpiryNotifier,
    private val scheduler: AndroidReminderScheduler,
    private val history: ReminderHistory,
    private val clock: Clock,
    private val log: EventLog,
) {
    /** Runs the check; [trigger] says what started it, for the diagnostics log. */
    suspend operator fun invoke(trigger: String) {
        log.record { "reminder check: started by $trigger" }
        val settings = settingsRepository.settings.first()
        if (!settings.remindersEnabled) {
            log.record { "reminder check: skipped, reminders are off" }
            return
        }
        val summary = getExpirySummary()
        val occurrence = lastOccurrence(settings.reminderTime, ZonedDateTime.now(clock))
        if (history.markShown(occurrence)) {
            log.record { "reminder check: ${summary.expired.size} expired, ${summary.expiringSoon.size} expiring soon" }
            notifier.showExpiryReminder(summary)
        } else {
            log.record { "reminder check: the reminder due at $occurrence was already shown" }
        }
        scheduler.scheduleNext(settings.reminderTime, settings.exactReminders)
    }
}
