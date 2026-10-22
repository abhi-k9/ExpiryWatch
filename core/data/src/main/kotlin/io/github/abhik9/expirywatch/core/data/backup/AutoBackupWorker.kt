package io.github.abhik9.expirywatch.core.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.domain.repository.AutoBackupScheduler
import io.github.abhik9.expirywatch.core.domain.usecase.BackupResult
import io.github.abhik9.expirywatch.core.domain.usecase.RunAutoBackupUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Makes the weekly automatic backup; see [WorkManagerAutoBackupScheduler]. */
@HiltWorker
class AutoBackupWorker @AssistedInject internal constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val runAutoBackup: RunAutoBackupUseCase,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val failed = runAutoBackup() == BackupResult.IoError
        // A failure shows in the settings; retrying helps when, say, the folder's on a card put back later.
        return if (failed && runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_RETRIES = 2
    }
}

/** Runs [AutoBackupWorker] weekly while the battery isn't low, the first time a week after it's set. */
internal class WorkManagerAutoBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : AutoBackupScheduler {
    override suspend fun scheduleWeekly() {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(BACKUP_INTERVAL_DAYS, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            // A backup is made when the folder is picked, so the next is due in a week.
            .setInitialDelay(BACKUP_INTERVAL_DAYS, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    override suspend fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "auto_backup"
        const val BACKUP_INTERVAL_DAYS = 7L
    }
}
