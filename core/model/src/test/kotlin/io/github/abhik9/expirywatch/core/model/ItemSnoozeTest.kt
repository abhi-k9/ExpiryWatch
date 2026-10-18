package io.github.abhik9.expirywatch.core.model

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemSnoozeTest {
    private val today = LocalDate.of(2026, 3, 10)

    private fun item(snoozedUntil: LocalDate?) =
        Item(name = "Honey", expiryDate = today.minusDays(30), remindersSnoozedUntil = snoozedUntil)

    @Test
    fun remindersStartAgainOnTheDayTheSnoozeEnds() {
        val honey = item(snoozedUntil = today.plusDays(1))

        assertTrue(honey.isReminderSnoozed(today))
        assertFalse(honey.isReminderSnoozed(today.plusDays(1)))
    }

    @Test
    fun anIndefiniteSnoozeNeverEnds() {
        assertTrue(item(snoozedUntil = Item.SNOOZED_INDEFINITELY).isReminderSnoozed(today.plusYears(100)))
    }

    @Test
    fun itemsAreNotSnoozedByDefault() {
        assertFalse(item(snoozedUntil = null).isReminderSnoozed(today))
    }
}
