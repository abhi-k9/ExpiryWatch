package io.github.abhik9.expirywatch.core.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.GetExpirySummaryUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeRemindersUseCase
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.diagnostics.RecordingEventLog
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ReminderActionsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settings = FakeUserSettingsRepository()
    private val log = RecordingEventLog()
    private val items = FakeItemRepository(
        listOf(
            TestData.item(id = 1, name = "Milk", expiresInDays = -2),
            TestData.item(id = 2, name = "Yogurt", expiresInDays = -1),
            TestData.item(id = 3, name = "Bread", expiresInDays = 2),
        ),
    )
    private val getExpirySummary = GetExpirySummaryUseCase(items, settings, ObserveTodayUseCase(TestTime.clock))
    private val notifier = ExpiryNotifier(context, log)
    private val actions = ReminderActions(
        settingsRepository = settings,
        getExpirySummary = getExpirySummary,
        itemRepository = items,
        finishItem = FinishItemUseCase(items, TestTime.clock),
        snoozeReminders = SnoozeRemindersUseCase(items),
        notifier = notifier,
        clock = TestTime.clock,
        log = log,
    )

    @Before
    fun setUp() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun shown() = NotificationManagerCompat.from(context).activeNotifications

    private fun summary() = shown().single { it.tag == null }.notification

    private fun itemNotifications() = shown().filter {
        it.tag != null
    }.map { it.notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString() }

    @Test
    fun eachExpiredItemGetsANotificationWithButtons() = runTest {
        notifier.showExpiryReminder(getExpirySummary())

        assertEquals(listOf("Milk", "Yogurt"), itemNotifications().sortedBy { it })
        assertEquals(1, summary().actions.size)
        shown().filter { it.tag != null }.forEach { assertEquals(3, it.notification.actions.size) }
    }

    @Test
    fun finishingAnItemRemovesItsNotificationAndUpdatesTheSummary() = runTest {
        notifier.showExpiryReminder(getExpirySummary())

        actions(ReminderActions.ACTION_USED_UP, itemId = 1)
        actions(ReminderActions.ACTION_THROWN_AWAY, itemId = 2)

        assertEquals(ItemStatus.CONSUMED, items.observeItem(1).first()?.status)
        assertEquals(ItemStatus.WASTED, items.observeItem(2).first()?.status)
        assertEquals(emptyList(), itemNotifications())
        assertEquals(
            "1 item needs your attention",
            summary().extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString(),
        )
    }

    @Test
    fun snoozingAllRemovesTheReminder() = runTest {
        notifier.showExpiryReminder(getExpirySummary())

        actions(ReminderActions.ACTION_SNOOZE_ALL, itemId = null)

        assertEquals(emptyList(), shown())
        assertEquals(TestTime.today.plusWeeks(1), items.observeItem(3).first()?.remindersSnoozedUntil)
    }

    @Test
    fun anActionDoesNotBringBackADismissedReminder() = runTest {
        notifier.showExpiryReminder(getExpirySummary())
        NotificationManagerCompat.from(context).cancelAll()

        actions(ReminderActions.ACTION_SNOOZE, itemId = 1)

        assertEquals(emptyList(), shown())
        assertEquals(TestTime.today.plusWeeks(1), items.observeItem(1).first()?.remindersSnoozedUntil)
    }

    @Test
    fun theActionDataCarriesTheItem() {
        assertEquals(42L, ReminderActions.itemIdFrom(ReminderActions.dataUri(42)))
        assertEquals(null, ReminderActions.itemIdFrom(ReminderActions.dataUri(null)))
    }
}
