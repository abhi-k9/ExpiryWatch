package io.github.abhik9.expirywatch.core.model

import java.time.LocalTime

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    /** Items expiring within this many days are flagged as "expiring soon" and reminded about. */
    val expiringSoonDays: Int = DEFAULT_EXPIRING_SOON_DAYS,
    val remindersEnabled: Boolean = true,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    val sortOrder: ItemSortOrder = ItemSortOrder.EXPIRY_SOONEST,
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

enum class ItemSortOrder {
    EXPIRY_SOONEST,
    EXPIRY_LATEST,
    NAME,
    RECENTLY_ADDED,
}
