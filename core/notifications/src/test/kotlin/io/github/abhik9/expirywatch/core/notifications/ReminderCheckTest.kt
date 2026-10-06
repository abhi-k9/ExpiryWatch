package io.github.abhik9.expirywatch.core.notifications

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.abhik9.expirywatch.core.domain.usecase.GetExpirySummaryUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.expirywatch.core.model.UserSettings
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.diagnostics.RecordingEventLog
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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

@RunWith(RobolectricTestRunner::class)
class ReminderCheckTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // WorkManager works with the real time, so the clock follows it, moved ahead by [daysLater].
    private var daysLater = 0L
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("America/Denver")

        override fun withZone(zone: ZoneId): Clock = throw UnsupportedOperationException()

        override fun instant(): Instant = Instant.now().plus(Duration.ofDays(daysLater))
    }

    // An hour ago, so every check in a test is for the same reminder.
    private val reminderTime = LocalTime.now(clock).minusHours(1).truncatedTo(ChronoUnit.MINUTES)
    private val settings = FakeUserSettingsRepository(UserSettings(reminderTime = reminderTime))
    private val log = RecordingEventLog()

    private val items by lazy {
        FakeItemRepository(listOf(TestData.item(id = 1, expiresInDays = -1, today = LocalDate.now(clock))))
    }

    private val check by lazy {
        ReminderCheck(
            settingsRepository = settings,
            getExpirySummary = GetExpirySummaryUseCase(items, settings, ObserveTodayUseCase(clock)),
            notifier = ExpiryNotifier(context, log),
            scheduler = AndroidReminderScheduler(context, ReminderAlarm(context), clock, log),
            history = ReminderHistory(context),
            clock = clock,
            log = log,
        )
    }

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun remindersShown() = log.messages.count { it.startsWith("reminder notification: shown") }

    private suspend fun nextCheck(): Instant? = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow(AndroidReminderScheduler.WORK_NAME)
        .first()
        .firstOrNull { !it.state.isFinished }
        ?.let { Instant.ofEpochMilli(it.nextScheduleTimeMillis) }

    @Test
    fun theAlarmAndTheDailyWorkShowEachReminderOnce() = runTest {
        check("the exact alarm")
        check("the daily work")

        assertEquals(1, remindersShown())
        val due = lastOccurrence(reminderTime, ZonedDateTime.now(clock))
        assertTrue("reminder check: the reminder due at $due was already shown" in log.messages)
    }

    @Test
    fun theNextDaysReminderIsShown() = runTest {
        check("the daily work")
        daysLater = 1

        check("the daily work")

        assertEquals(2, remindersShown())
    }

    @Test
    fun aReminderMovedToAnotherTimeIsShown() = runTest {
        check("the daily work")
        settings.setReminderTime(reminderTime.plusMinutes(30))

        check("the daily work")

        assertEquals(2, remindersShown())
    }

    @Test
    fun eachCheckSetsTheNextOne() = runTest {
        check("the exact alarm")

        assertEquals(nextOccurrence(reminderTime, ZonedDateTime.now(clock)).toInstant(), nextCheck())
    }

    @Test
    fun nothingIsShownWhileRemindersAreOff() = runTest {
        settings.setRemindersEnabled(false)

        check("the daily work")

        assertEquals(0, remindersShown())
        assertEquals(null, nextCheck())
    }

    @Test
    fun expiredItemsCanBeLeftOutOfTheReminder() = runTest {
        settings.setRemindAboutExpired(false)

        check("the daily work")
        assertEquals(0, remindersShown())
        assertTrue("reminder notification: removed, nothing needs attention" in log.messages)

        // The next day, something is about to expire too.
        items.upsert(TestData.item(name = "Bread", expiresInDays = 2, today = LocalDate.now(clock).plusDays(1)))
        daysLater = 1
        check("the daily work")
        assertTrue("reminder notification: shown for 1 items" in log.messages)
    }
}
