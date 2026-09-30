package io.github.abhik9.expirywatch.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.domain.repository.ReminderScheduler
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

internal class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : ReminderScheduler {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override suspend fun scheduleDaily(time: LocalTime) {
        val timeTag = "$TAG_PREFIX$time"
        val existing = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).first()
        if (existing.any { !it.state.isFinished && timeTag in it.tags }) return

        val now = LocalDateTime.now(clock)
        val todayRun = now.toLocalDate().atTime(time)
        val nextRun = if (todayRun.isAfter(now)) todayRun else todayRun.plusDays(1)

        val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(Duration.between(now, nextRun))
            .addTag(timeTag)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    override suspend fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "expiry_reminder"
        const val TAG_PREFIX = "reminder_at_"
    }
}
