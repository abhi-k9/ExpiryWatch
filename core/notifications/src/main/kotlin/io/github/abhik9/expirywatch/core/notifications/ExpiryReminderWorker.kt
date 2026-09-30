package io.github.abhik9.expirywatch.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (settingsRepository.settings.first().remindersEnabled) {
            notifier.showExpiryReminder(getExpirySummary())
        }
        return Result.success()
    }
}
