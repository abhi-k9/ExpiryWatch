package io.github.abhik9.expirywatch.core.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.repository.ReminderScheduler
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Runs the daily check as periodic work, pinned to the reminder time. Periodic work counts each
 * period from the previous run, so a run that comes late (for example after the system stopped
 * the app) would otherwise move every later reminder too. Each run therefore sets the next one.
 */
internal class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    private val log: EventLog,
) : ReminderScheduler {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override suspend fun scheduleDaily(time: LocalTime) {
        val pending = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).first().firstOrNull { !it.state.isFinished }
        val now = ZonedDateTime.now(clock)
        if (pending != null && pending.isOnSchedule(time, now)) {
            log.record { "reminders: daily at $time already scheduled" }
            return
        }
        scheduleNext(time)
    }

    /** Sets the next check to the next [time], whatever is scheduled now. */
    fun scheduleNext(time: LocalTime) {
        val nextRun = nextOccurrence(time, ZonedDateTime.now(clock))
        val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(Duration.ofDays(1))
            .setNextScheduleTimeOverride(nextRun.toInstant().toEpochMilli())
            .build()
        // UPDATE rather than replacing the work, so a check that is running isn't cancelled.
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        log.record { "reminders: next check at $nextRun" }
    }

    override suspend fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
        log.record { "reminders: cancelled" }
    }

    private fun WorkInfo.isOnSchedule(time: LocalTime, now: ZonedDateTime): Boolean {
        // A running check schedules the next one when it's done.
        if (state == WorkInfo.State.RUNNING) return true
        if (nextScheduleTimeMillis == Long.MAX_VALUE) return false
        val nextRun = Instant.ofEpochMilli(nextScheduleTimeMillis).atZone(now.zone)
        return isOnSchedule(nextRun, time, now)
    }

    companion object {
        const val WORK_NAME = "expiry_reminder"
    }
}

/**
 * Whether a check due at [nextRun] suits a daily reminder at [time]: it's the next one, or it's
 * overdue and about to run, for example after the device was off.
 */
internal fun isOnSchedule(nextRun: ZonedDateTime, time: LocalTime, now: ZonedDateTime): Boolean =
    !nextRun.isAfter(now) ||
        nextRun.truncatedTo(ChronoUnit.MINUTES) == nextOccurrence(time, now).truncatedTo(ChronoUnit.MINUTES)

/** The next moment after [now] at [time]; on a day when the clocks skip [time], just after the gap. */
internal fun nextOccurrence(time: LocalTime, now: ZonedDateTime): ZonedDateTime {
    val today = now.with(time)
    return if (today.isAfter(now)) today else now.plusDays(1).with(time)
}
