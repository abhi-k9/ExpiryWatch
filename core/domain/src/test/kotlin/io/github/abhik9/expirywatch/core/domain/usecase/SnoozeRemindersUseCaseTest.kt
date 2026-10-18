package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SnoozeRemindersUseCaseTest {
    private val honey = TestData.item(id = 1, name = "Honey", expiresInDays = -30)
        .copy(remindersSnoozedUntil = Item.SNOOZED_INDEFINITELY)
    private val milk = TestData.item(id = 2, name = "Milk", expiresInDays = -1)
    private val bread = TestData.item(id = 3, name = "Bread", expiresInDays = -2)
        .copy(remindersSnoozedUntil = TestTime.today.plusDays(2))
    private val items = FakeItemRepository(listOf(honey, milk, bread))
    private val snoozeReminders = SnoozeRemindersUseCase(items)

    private fun snoozes(): Map<String, LocalDate?> =
        items.currentItems.associate { it.name to it.remindersSnoozedUntil }

    @Test
    fun snoozesAndUndoesForEachItemAsItWas() = runTest {
        val until = SnoozeLength.ONE_WEEK.resumeDate(TestTime.today)

        snoozeReminders(listOf(honey, milk, bread), until)
        assertEquals(mapOf("Honey" to until, "Milk" to until, "Bread" to until), snoozes())

        snoozeReminders.restore(listOf(honey, milk, bread))
        assertEquals(
            mapOf("Honey" to Item.SNOOZED_INDEFINITELY, "Milk" to null, "Bread" to TestTime.today.plusDays(2)),
            snoozes(),
        )
    }

    @Test
    fun turnsRemindersBackOn() = runTest {
        snoozeReminders(listOf(honey), until = null)

        assertEquals(null, snoozes()["Honey"])
    }

    @Test
    fun lengthsEndOnTheDayRemindersStartAgain() {
        val today = LocalDate.of(2026, 1, 31)

        assertEquals(LocalDate.of(2026, 2, 7), SnoozeLength.ONE_WEEK.resumeDate(today))
        assertEquals(LocalDate.of(2026, 2, 28), SnoozeLength.ONE_MONTH.resumeDate(today))
        assertEquals(Item.SNOOZED_INDEFINITELY, SnoozeLength.INDEFINITELY.resumeDate(today))
    }
}
