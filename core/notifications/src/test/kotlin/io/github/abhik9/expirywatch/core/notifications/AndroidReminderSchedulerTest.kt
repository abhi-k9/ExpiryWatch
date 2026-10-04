package io.github.abhik9.expirywatch.core.notifications

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.abhik9.expirywatch.core.testing.diagnostics.RecordingEventLog
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class AndroidReminderSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // WorkManager works with the real time, so the tests do too.
    private val clock = Clock.system(ZoneId.of("America/Denver"))
    private val log = RecordingEventLog()
    private val scheduler = AndroidReminderScheduler(context, ReminderAlarm(context), clock, log)
    private val workManager get() = WorkManager.getInstance(context)
    private val alarmManager get() = checkNotNull(context.getSystemService(AlarmManager::class.java))

    // Far from now, so the test can't straddle the reminder time.
    private val reminderTime = LocalTime.now(clock).plusHours(5).truncatedTo(ChronoUnit.MINUTES)

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    private suspend fun pendingWork(): WorkInfo? = workManager
        .getWorkInfosForUniqueWorkFlow(AndroidReminderScheduler.WORK_NAME)
        .first()
        .firstOrNull { !it.state.isFinished }

    private suspend fun nextRun(): Instant = Instant.ofEpochMilli(pendingWork()!!.nextScheduleTimeMillis)

    private fun expectedNextRun(time: LocalTime): Instant = nextOccurrence(time, ZonedDateTime.now(clock)).toInstant()

    private fun scheduledAlarms(): List<ShadowAlarmManager.ScheduledAlarm> = shadowOf(alarmManager).scheduledAlarms

    @Test
    fun schedulesTheNextCheckAtTheReminderTime() = runTest {
        scheduler.scheduleDaily(reminderTime, exact = false)

        assertEquals(expectedNextRun(reminderTime), nextRun())
    }

    @Test
    fun keepsACheckThatIsOnSchedule() = runTest {
        scheduler.scheduleDaily(reminderTime, exact = false)
        log.messages.clear()

        scheduler.scheduleDaily(reminderTime, exact = false)

        assertEquals(listOf("reminders: daily at $reminderTime already scheduled"), log.messages)
        assertEquals(expectedNextRun(reminderTime), nextRun())
    }

    @Test
    fun movesADriftedCheckBackToTheReminderTime() = runTest {
        // As left by a check that ran hours late, e.g. after the system stopped the app.
        val drifted = expectedNextRun(reminderTime).plus(Duration.ofMinutes(197))
        workManager.enqueueUniquePeriodicWork(
            AndroidReminderScheduler.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ExpiryReminderWorker>(Duration.ofDays(1))
                .setNextScheduleTimeOverride(drifted.toEpochMilli())
                .build(),
        )
        assertEquals(drifted, nextRun())

        scheduler.scheduleDaily(reminderTime, exact = false)

        assertEquals(expectedNextRun(reminderTime), nextRun())
    }

    @Test
    fun followsAChangedReminderTime() = runTest {
        scheduler.scheduleDaily(reminderTime, exact = false)
        val later = reminderTime.plusMinutes(90)

        scheduler.scheduleDaily(later, exact = false)

        assertEquals(expectedNextRun(later), nextRun())
    }

    @Test
    fun cancellingRemovesTheCheckAndTheAlarm() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler.scheduleDaily(reminderTime, exact = true)

        scheduler.cancel()

        assertEquals(null, pendingWork())
        assertEquals(emptyList(), scheduledAlarms())
        assertEquals("reminders: cancelled", log.messages.last())
    }

    @Test
    fun exactRemindersSetAnAlarmAtTheReminderTimeWhenAllowed() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        scheduler.scheduleDaily(reminderTime, exact = true)

        val alarm = scheduledAlarms().single()
        assertEquals(expectedNextRun(reminderTime).toEpochMilli(), alarm.triggerAtMs)
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.getType())
        assertTrue(alarm.isAllowWhileIdle)
        // The daily work stays, in case the alarm doesn't go off.
        assertEquals(expectedNextRun(reminderTime), nextRun())
    }

    @Test
    fun exactRemindersFallBackToTheDailyWorkWhenAlarmsAreNotAllowed() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        scheduler.scheduleDaily(reminderTime, exact = true)

        assertEquals(emptyList(), scheduledAlarms())
        assertEquals(expectedNextRun(reminderTime), nextRun())
        assertTrue("reminders: exact alarms aren't allowed, the check may come late" in log.messages)
    }

    @Test
    fun theAlarmIsSetAgainEvenWhenTheWorkIsOnSchedule() = runTest {
        // As after a restart, which clears alarms but not work.
        scheduler.scheduleDaily(reminderTime, exact = false)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        scheduler.scheduleDaily(reminderTime, exact = true)

        assertEquals(expectedNextRun(reminderTime).toEpochMilli(), scheduledAlarms().single().triggerAtMs)
    }

    @Test
    fun theAlarmFollowsAChangedReminderTime() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler.scheduleDaily(reminderTime, exact = true)
        val later = reminderTime.plusMinutes(90)

        scheduler.scheduleNext(later, exact = true)

        assertEquals(expectedNextRun(later).toEpochMilli(), scheduledAlarms().single().triggerAtMs)
        assertEquals(expectedNextRun(later), nextRun())
    }

    @Test
    fun turningExactRemindersOffRemovesTheAlarm() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler.scheduleDaily(reminderTime, exact = true)

        scheduler.scheduleDaily(reminderTime, exact = false)

        assertEquals(emptyList(), scheduledAlarms())
        assertEquals(expectedNextRun(reminderTime), nextRun())
    }
}
