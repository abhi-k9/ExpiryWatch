package io.github.abhik9.expirywatch.core.notifications

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkManagerReminderSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // WorkManager works with the real time, so the tests do too.
    private val clock = Clock.system(ZoneId.of("America/Denver"))
    private val log = RecordingEventLog()
    private val scheduler = WorkManagerReminderScheduler(context, clock, log)
    private val workManager get() = WorkManager.getInstance(context)

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
        .getWorkInfosForUniqueWorkFlow(WorkManagerReminderScheduler.WORK_NAME)
        .first()
        .firstOrNull { !it.state.isFinished }

    private suspend fun nextRun(): Instant = Instant.ofEpochMilli(pendingWork()!!.nextScheduleTimeMillis)

    private fun expectedNextRun(time: LocalTime): Instant = nextOccurrence(time, ZonedDateTime.now(clock)).toInstant()

    @Test
    fun schedulesTheNextCheckAtTheReminderTime() = runTest {
        scheduler.scheduleDaily(reminderTime)

        assertEquals(expectedNextRun(reminderTime), nextRun())
    }

    @Test
    fun keepsACheckThatIsOnSchedule() = runTest {
        scheduler.scheduleDaily(reminderTime)
        log.messages.clear()

        scheduler.scheduleDaily(reminderTime)

        assertEquals(listOf("reminders: daily at $reminderTime already scheduled"), log.messages)
        assertEquals(expectedNextRun(reminderTime), nextRun())
    }

    @Test
    fun movesADriftedCheckBackToTheReminderTime() = runTest {
        // As left by a check that ran hours late, e.g. after the system stopped the app.
        val drifted = expectedNextRun(reminderTime).plus(Duration.ofMinutes(197))
        workManager.enqueueUniquePeriodicWork(
            WorkManagerReminderScheduler.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ExpiryReminderWorker>(Duration.ofDays(1))
                .setNextScheduleTimeOverride(drifted.toEpochMilli())
                .build(),
        )
        assertEquals(drifted, nextRun())

        scheduler.scheduleDaily(reminderTime)

        assertEquals(expectedNextRun(reminderTime), nextRun())
    }

    @Test
    fun followsAChangedReminderTime() = runTest {
        scheduler.scheduleDaily(reminderTime)
        val later = reminderTime.plusMinutes(90)

        scheduler.scheduleDaily(later)

        assertEquals(expectedNextRun(later), nextRun())
    }

    @Test
    fun cancellingRemovesTheCheck() = runTest {
        scheduler.scheduleDaily(reminderTime)

        scheduler.cancel()

        assertEquals(null, pendingWork())
        assertEquals("reminders: cancelled", log.messages.last())
    }
}
