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
 *
 * The system may also run the work late to save battery, especially while the device is idle. For
 * exact reminders, an alarm runs the check at the reminder time too; the work stays as the fallback
 * for when the user doesn't allow alarms, and [ReminderCheck] shows each reminder only once.
 */
internal class AndroidReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarm: ReminderAlarm,
    private val clock: Clock,
    private val log: EventLog,
) : ReminderScheduler {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    override suspend fun scheduleDaily(time: LocalTime, exact: Boolean) {
        val now = ZonedDateTime.now(clock)
        // There's no way to look up a pending alarm, so it's always set again, replacing the old one.
        updateAlarm(time, exact, now)
        val pending = workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).first().firstOrNull { !it.state.isFinished }
        if (pending != null && pending.isOnSchedule(time, now)) {
            log.record { "reminders: daily at $time already scheduled" }
            return
        }
        scheduleWork(time, now)
    }

    /** Sets the next check to the next [time], whatever is scheduled now. */
    fun scheduleNext(time: LocalTime, exact: Boolean) {
        val now = ZonedDateTime.now(clock)
        scheduleWork(time, now)
        updateAlarm(time, exact, now)
    }

    override suspend fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
        alarm.cancel()
        log.record { "reminders: cancelled" }
    }

    private fun scheduleWork(time: LocalTime, now: ZonedDateTime) {
        val nextRun = nextOccurrence(time, now)
        val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(Duration.ofDays(1))
            .setNextScheduleTimeOverride(nextRun.toInstant().toEpochMilli())
            .build()
        // UPDATE rather than replacing the work, so a check that is running isn't cancelled.
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        log.record { "reminders: next check at $nextRun" }
    }

    private fun updateAlarm(time: LocalTime, exact: Boolean, now: ZonedDateTime) {
        if (!exact) {
            alarm.cancel()
            return
        }
        val nextRun = nextOccurrence(time, now)
        if (alarm.set(nextRun)) {
            log.record { "reminders: exact alarm at $nextRun" }
        } else {
            log.record { "reminders: exact alarms aren't allowed, the check may come late" }
        }
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

/**
 * The latest moment at or before [now] at [time]: the reminder that a check running now is for,
 * since checks never run early.
 */
internal fun lastOccurrence(time: LocalTime, now: ZonedDateTime): ZonedDateTime {
    val today = now.with(time)
    return if (today.isAfter(now)) now.minusDays(1).with(time) else today
}

/** The next moment after [now] at [time]; on a day when the clocks skip [time], just after the gap. */
internal fun nextOccurrence(time: LocalTime, now: ZonedDateTime): ZonedDateTime {
    val today = now.with(time)
    return if (today.isAfter(now)) today else now.plusDays(1).with(time)
}
