package io.github.abhik9.expirywatch.core.model

import java.time.LocalTime

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    /** Items expiring within this many days are flagged as "expiring soon" and reminded about. */
    val expiringSoonDays: Int = DEFAULT_EXPIRING_SOON_DAYS,
    val remindersEnabled: Boolean = true,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    /** Show the reminder at exactly [reminderTime] with an alarm, if the system allows the app to set one. */
    val exactReminders: Boolean = false,
    val sortOrder: ItemSortOrder = ItemSortOrder.EXPIRY_SOONEST,
    /** Which items the list folds into one row as the same product. */
    val productGrouping: ProductGrouping = ProductGrouping.NAME,
    /**
     * Keep all of a product's items in one group, under the status of the one that expires first,
     * rather than a group in each status.
     */
    val keepProductsTogether: Boolean = false,
    /** List expired items in the daily reminder, not just those about to expire. */
    val remindAboutExpired: Boolean = true,
    /** Look up unknown barcodes in the online product catalog. */
    val onlineProductLookup: Boolean = true,
) {
    companion object {
        const val DEFAULT_EXPIRING_SOON_DAYS = 3
        val EXPIRING_SOON_DAYS_RANGE = 1..14
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** What makes items the same product, to show them together in the list. */
enum class ProductGrouping {
    /** Every item has a row of its own. */
    OFF,

    /** The same name, ignoring case, accents and spacing. */
    NAME,

    /** The same name and brand. */
    NAME_AND_BRAND,
}

enum class ItemSortOrder {
    EXPIRY_SOONEST,
    EXPIRY_LATEST,
    NAME,
    RECENTLY_ADDED,
}
