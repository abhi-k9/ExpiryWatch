package io.github.abhik9.expirywatch.core.model

import java.time.Instant
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
    /** The reminder's "Snooze all" also snoozes the items about to expire, not just the expired ones. */
    val snoozeAllIncludesExpiringSoon: Boolean = true,
    /** Look up unknown barcodes in the online product catalog. */
    val onlineProductLookup: Boolean = true,
    /** The shopping list last opened; `null` for the first one. */
    val shoppingListId: Long? = null,
    /** The suggestions to buy something again that the user dismissed. */
    val dismissedRestockSuggestions: Set<String> = emptySet(),
    /** Category ids in the order the user walks through the shop; categories left out come after, by name. */
    val shoppingAisleOrder: List<Long> = emptyList(),
    /** Keep the screen from turning off while the shopping list is open. */
    val keepScreenOnWhileShopping: Boolean = false,
    /** The folder, a document tree URI, to back up to every week; `null` while automatic backups are off. */
    val autoBackupFolder: String? = null,
    /** How the last automatic backup to [autoBackupFolder] went; `null` before the first. */
    val lastAutoBackup: AutoBackupRun? = null,
) {
    companion object {
        const val DEFAULT_EXPIRING_SOON_DAYS = 3
        val EXPIRING_SOON_DAYS_RANGE = 1..14
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)
    }
}

/** An automatic backup that ran [at] a time, and whether it [failed]. */
data class AutoBackupRun(val at: Instant, val failed: Boolean)

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
