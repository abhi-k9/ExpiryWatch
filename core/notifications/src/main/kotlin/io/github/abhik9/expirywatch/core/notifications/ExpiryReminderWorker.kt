package io.github.abhik9.expirywatch.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.usecase.GetExpirySummaryUseCase
import kotlinx.coroutines.flow.first

/** Runs once a day and notifies the user about expired and soon-to-expire items. */
@HiltWorker
class ExpiryReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: UserSettingsRepository,
    private val getExpirySummary: GetExpirySummaryUseCase,
    private val notifier: ExpiryNotifier,
    private val log: EventLog,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        log.record { "reminder check: started, attempt ${runAttemptCount + 1}" }
        if (settingsRepository.settings.first().remindersEnabled) {
            val summary = getExpirySummary()
            log.record { "reminder check: ${summary.expired.size} expired, ${summary.expiringSoon.size} expiring soon" }
            notifier.showExpiryReminder(summary)
        } else {
            log.record { "reminder check: skipped, reminders are off" }
        }
        return Result.success()
    }
}
