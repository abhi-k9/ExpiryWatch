package io.github.abhik9.expirywatch.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs the reminder check once a day; see [AndroidReminderScheduler]. */
@HiltWorker
class ExpiryReminderWorker @AssistedInject internal constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val check: ReminderCheck,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        check("the daily work, attempt ${runAttemptCount + 1}")
        return Result.success()
    }
}
