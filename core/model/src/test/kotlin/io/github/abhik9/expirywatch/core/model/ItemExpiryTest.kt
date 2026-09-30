package io.github.abhik9.expirywatch.core.model

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemExpiryTest {
    private val today = LocalDate.of(2026, 3, 10)

    private fun item(
        expiry: LocalDate,
        opened: LocalDate? = null,
        useWithinDays: Int? = null,
    ) = Item(name = "Milk", expiryDate = expiry, openedDate = opened, useWithinDaysAfterOpening = useWithinDays)

    @Test
    fun effectiveExpiryIsPrintedDateWhenUnopened() {
        val milk = item(expiry = today.plusDays(10), useWithinDays = 3)
        assertEquals(today.plusDays(10), milk.effectiveExpiryDate)
    }

    @Test
    fun openingBringsExpiryForwardWhenWindowEndsFirst() {
        val milk = item(expiry = today.plusDays(10), opened = today, useWithinDays = 3)
        assertEquals(today.plusDays(3), milk.effectiveExpiryDate)
    }

    @Test
    fun printedDateWinsWhenItComesBeforeOpenedWindow() {
        val milk = item(expiry = today.plusDays(1), opened = today, useWithinDays = 5)
        assertEquals(today.plusDays(1), milk.effectiveExpiryDate)
    }

    @Test
    fun openedWithoutWindowKeepsPrintedDate() {
        val milk = item(expiry = today.plusDays(4), opened = today)
        assertEquals(today.plusDays(4), milk.effectiveExpiryDate)
    }

    @Test
    fun statusBoundaries() {
        val soonDays = 3
        assertEquals(ExpiryStatus.EXPIRED, item(today.minusDays(1)).expiryStatus(today, soonDays))
        assertEquals(ExpiryStatus.EXPIRING_SOON, item(today).expiryStatus(today, soonDays))
        assertEquals(ExpiryStatus.EXPIRING_SOON, item(today.plusDays(3)).expiryStatus(today, soonDays))
        assertEquals(ExpiryStatus.FRESH, item(today.plusDays(4)).expiryStatus(today, soonDays))
    }

    @Test
    fun daysUntilExpiryIsNegativeForExpiredItems() {
        assertEquals(-2, item(today.minusDays(2)).daysUntilExpiry(today))
        assertEquals(0, item(today).daysUntilExpiry(today))
        assertEquals(5, item(today.plusDays(5)).daysUntilExpiry(today))
    }
}
