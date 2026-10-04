package io.github.abhik9.expirywatch.core.domain.usecase

import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.UserSettings
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeReminderScheduler
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class ItemLifecycleUseCasesTest {
    private val items = FakeItemRepository(listOf(TestData.item(id = 1)))

    @Test
    fun finishingAnItemRecordsOutcomeAndDate() = runTest {
        FinishItemUseCase(items, TestTime.clock)(1, ItemStatus.WASTED)

        val item = items.currentItems.single()
        assertEquals(ItemStatus.WASTED, item.status)
        assertEquals(TestTime.today, item.finishedDate)
    }

    @Test
    fun finishingRequiresAFinalOutcome() = runTest {
        assertFailsWith<IllegalArgumentException> {
            FinishItemUseCase(items, TestTime.clock)(1, ItemStatus.ACTIVE)
        }
    }

    @Test
    fun restoringUndoesFinishing() = runTest {
        FinishItemUseCase(items, TestTime.clock)(1, ItemStatus.CONSUMED)
        RestoreItemUseCase(items)(1)

        val item = items.currentItems.single()
        assertEquals(ItemStatus.ACTIVE, item.status)
        assertNull(item.finishedDate)
    }

    @Test
    fun expirySummarySplitsExpiredAndExpiringSoon() = runTest {
        items.setItems(
            listOf(
                TestData.item(id = 1, name = "Old", expiresInDays = -1),
                TestData.item(id = 2, name = "Soon", expiresInDays = 2),
                TestData.item(id = 3, name = "Later", expiresInDays = 20),
            ),
        )
        val settings = FakeUserSettingsRepository(UserSettings(expiringSoonDays = 3))

        val summary = GetExpirySummaryUseCase(items, settings, ObserveTodayUseCase(TestTime.clock))()

        assertEquals(listOf("Old"), summary.expired.map { it.item.name })
        assertEquals(listOf("Soon"), summary.expiringSoon.map { it.item.name })
        assertEquals(listOf("Old", "Soon", "Later"), summary.items.map { it.item.name })
    }

    @Test
    fun observedExpirySummaryFollowsItemsAndSettings() = runTest {
        items.setItems(listOf(TestData.item(id = 1, name = "Milk", expiresInDays = 5)))
        val settings = FakeUserSettingsRepository(UserSettings(expiringSoonDays = 3))
        val useCase = GetExpirySummaryUseCase(items, settings, ObserveTodayUseCase(TestTime.clock))

        useCase.observe().test {
            assertEquals(emptyList(), awaitItem().expiringSoon)

            settings.setExpiringSoonDays(7)
            assertEquals(listOf("Milk"), awaitItem().expiringSoon.map { it.item.name })

            items.setItems(emptyList())
            assertEquals(emptyList(), awaitItem().items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun reminderScheduleFollowsSettings() = runTest {
        val settings =
            FakeUserSettingsRepository(UserSettings(remindersEnabled = true, reminderTime = LocalTime.of(9, 0)))
        val scheduler = FakeReminderScheduler()
        val job = launch { SyncReminderScheduleUseCase(settings, scheduler)() }

        advanceUntilIdle()
        assertEquals(LocalTime.of(9, 0), scheduler.scheduledTime)
        assertFalse(scheduler.exact)

        settings.setReminderTime(LocalTime.of(18, 30))
        advanceUntilIdle()
        assertEquals(LocalTime.of(18, 30), scheduler.scheduledTime)

        settings.setExactReminders(true)
        advanceUntilIdle()
        assertEquals(LocalTime.of(18, 30), scheduler.scheduledTime)
        assertTrue(scheduler.exact)

        // Unrelated changes don't reschedule.
        val callsBefore = scheduler.scheduleCalls
        settings.setExpiringSoonDays(5)
        advanceUntilIdle()
        assertEquals(callsBefore, scheduler.scheduleCalls)

        settings.setRemindersEnabled(false)
        advanceUntilIdle()
        assertNull(scheduler.scheduledTime)

        job.cancel()
    }

    @Test
    fun reminderScheduleCanBeAppliedAgainWithoutAChange() = runTest {
        val settings = FakeUserSettingsRepository(
            UserSettings(remindersEnabled = true, reminderTime = LocalTime.of(7, 15), exactReminders = true),
        )
        val scheduler = FakeReminderScheduler()
        val sync = SyncReminderScheduleUseCase(settings, scheduler)

        sync.once()
        sync.once()

        assertEquals(2, scheduler.scheduleCalls)
        assertEquals(LocalTime.of(7, 15), scheduler.scheduledTime)
        assertTrue(scheduler.exact)

        settings.setRemindersEnabled(false)
        sync.once()

        assertNull(scheduler.scheduledTime)
    }
}
