package io.github.abhik9.expirywatch.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How close an item is to its expiry date. */
enum class ExpiryStatus {
    EXPIRED,
    EXPIRING_SOON,
    FRESH,
}

/**
 * The date the item should be used by: the printed [Item.expiryDate], or the end of its
 * "use within N days of opening" window if that comes first.
 */
val Item.effectiveExpiryDate: LocalDate
    get() {
        val opened = openedDate ?: return expiryDate
        val keepsFor = useWithinDaysAfterOpening ?: return expiryDate
        val openedExpiry = opened.plusDays(keepsFor.toLong())
        return minOf(expiryDate, openedExpiry)
    }

/** Whole days from [today] until [effectiveExpiryDate]: 0 is today, negative is in the past. */
fun Item.daysUntilExpiry(today: LocalDate): Long = ChronoUnit.DAYS.between(today, effectiveExpiryDate)

/**
 * Classifies the item relative to [today]. An item that expires today counts as expiring soon,
 * not expired, since it can still be used.
 *
 * @param expiringSoonDays how many days ahead count as "soon".
 */
fun Item.expiryStatus(today: LocalDate, expiringSoonDays: Int): ExpiryStatus {
    val days = daysUntilExpiry(today)
    return when {
        days < 0 -> ExpiryStatus.EXPIRED
        days <= expiringSoonDays -> ExpiryStatus.EXPIRING_SOON
        else -> ExpiryStatus.FRESH
    }
}
