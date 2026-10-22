package io.github.abhik9.expirywatch.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.abhik9.expirywatch.core.model.AutoBackupRun
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ProductGrouping
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.model.UserSettings
import java.io.IOException
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Reads and writes [UserSettings] in a Preferences DataStore. Missing values use the defaults. */
class UserSettingsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<UserSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toUserSettings() }

    suspend fun setThemeMode(themeMode: ThemeMode) = dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }

    suspend fun setUseDynamicColor(useDynamicColor: Boolean) =
        dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = useDynamicColor }

    suspend fun setExpiringSoonDays(days: Int) = dataStore.edit {
        it[Keys.EXPIRING_SOON_DAYS] = days.coerceIn(UserSettings.EXPIRING_SOON_DAYS_RANGE)
    }

    suspend fun setRemindersEnabled(enabled: Boolean) = dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }

    suspend fun setReminderTime(time: LocalTime) = dataStore.edit {
        it[Keys.REMINDER_MINUTE_OF_DAY] = time.hour * MINUTES_PER_HOUR + time.minute
    }

    suspend fun setExactReminders(exact: Boolean) = dataStore.edit { it[Keys.EXACT_REMINDERS] = exact }

    suspend fun setSortOrder(sortOrder: ItemSortOrder) = dataStore.edit { it[Keys.SORT_ORDER] = sortOrder.name }

    suspend fun setProductGrouping(grouping: ProductGrouping) =
        dataStore.edit { it[Keys.PRODUCT_GROUPING] = grouping.name }

    suspend fun setKeepProductsTogether(keepTogether: Boolean) =
        dataStore.edit { it[Keys.KEEP_PRODUCTS_TOGETHER] = keepTogether }

    suspend fun setRemindAboutExpired(remind: Boolean) = dataStore.edit { it[Keys.REMIND_ABOUT_EXPIRED] = remind }

    suspend fun setSnoozeAllIncludesExpiringSoon(include: Boolean) =
        dataStore.edit { it[Keys.SNOOZE_ALL_INCLUDES_EXPIRING_SOON] = include }

    suspend fun setOnlineProductLookup(enabled: Boolean) = dataStore.edit { it[Keys.ONLINE_PRODUCT_LOOKUP] = enabled }

    suspend fun setShoppingListId(id: Long) = dataStore.edit { it[Keys.SHOPPING_LIST_ID] = id }

    suspend fun setDismissedRestockSuggestions(keys: Set<String>) =
        dataStore.edit { it[Keys.DISMISSED_RESTOCK_SUGGESTIONS] = keys }

    suspend fun setShoppingAisleOrder(categoryIds: List<Long>) =
        dataStore.edit { it[Keys.SHOPPING_AISLE_ORDER] = categoryIds.joinToString(",") }

    suspend fun setKeepScreenOnWhileShopping(keepOn: Boolean) =
        dataStore.edit { it[Keys.KEEP_SCREEN_ON_WHILE_SHOPPING] = keepOn }

    /** Sets the folder to back up to, or turns automatic backups off; either forgets the last run. */
    suspend fun setAutoBackupFolder(uri: String?) = dataStore.edit {
        if (uri == null) it.remove(Keys.AUTO_BACKUP_FOLDER) else it[Keys.AUTO_BACKUP_FOLDER] = uri
        it.remove(Keys.LAST_AUTO_BACKUP_AT)
        it.remove(Keys.LAST_AUTO_BACKUP_FAILED)
    }

    suspend fun setLastAutoBackup(run: AutoBackupRun) = dataStore.edit {
        it[Keys.LAST_AUTO_BACKUP_AT] = run.at.toEpochMilli()
        it[Keys.LAST_AUTO_BACKUP_FAILED] = run.failed
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            themeMode = enumOrDefault(this[Keys.THEME_MODE], defaults.themeMode),
            useDynamicColor = this[Keys.USE_DYNAMIC_COLOR] ?: defaults.useDynamicColor,
            expiringSoonDays = (this[Keys.EXPIRING_SOON_DAYS] ?: defaults.expiringSoonDays)
                .coerceIn(UserSettings.EXPIRING_SOON_DAYS_RANGE),
            remindersEnabled = this[Keys.REMINDERS_ENABLED] ?: defaults.remindersEnabled,
            reminderTime = this[Keys.REMINDER_MINUTE_OF_DAY]
                ?.takeIf { it in 0 until MINUTES_PER_DAY }
                ?.let { LocalTime.of(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR) }
                ?: defaults.reminderTime,
            exactReminders = this[Keys.EXACT_REMINDERS] ?: defaults.exactReminders,
            sortOrder = enumOrDefault(this[Keys.SORT_ORDER], defaults.sortOrder),
            productGrouping = enumOrDefault(this[Keys.PRODUCT_GROUPING], defaults.productGrouping),
            keepProductsTogether = this[Keys.KEEP_PRODUCTS_TOGETHER] ?: defaults.keepProductsTogether,
            remindAboutExpired = this[Keys.REMIND_ABOUT_EXPIRED] ?: defaults.remindAboutExpired,
            snoozeAllIncludesExpiringSoon = this[Keys.SNOOZE_ALL_INCLUDES_EXPIRING_SOON]
                ?: defaults.snoozeAllIncludesExpiringSoon,
            onlineProductLookup = this[Keys.ONLINE_PRODUCT_LOOKUP] ?: defaults.onlineProductLookup,
            shoppingListId = this[Keys.SHOPPING_LIST_ID] ?: defaults.shoppingListId,
            dismissedRestockSuggestions = this[Keys.DISMISSED_RESTOCK_SUGGESTIONS]
                ?: defaults.dismissedRestockSuggestions,
            shoppingAisleOrder = this[Keys.SHOPPING_AISLE_ORDER]
                ?.split(',')
                ?.mapNotNull { it.trim().toLongOrNull() }
                ?: defaults.shoppingAisleOrder,
            keepScreenOnWhileShopping = this[Keys.KEEP_SCREEN_ON_WHILE_SHOPPING] ?: defaults.keepScreenOnWhileShopping,
            autoBackupFolder = this[Keys.AUTO_BACKUP_FOLDER],
            lastAutoBackup = this[Keys.LAST_AUTO_BACKUP_AT]?.let {
                AutoBackupRun(at = Instant.ofEpochMilli(it), failed = this[Keys.LAST_AUTO_BACKUP_FAILED] ?: false)
            },
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val EXPIRING_SOON_DAYS = intPreferencesKey("expiring_soon_days")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_MINUTE_OF_DAY = intPreferencesKey("reminder_minute_of_day")
        val EXACT_REMINDERS = booleanPreferencesKey("exact_reminders")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val PRODUCT_GROUPING = stringPreferencesKey("product_grouping")
        val KEEP_PRODUCTS_TOGETHER = booleanPreferencesKey("keep_products_together")
        val REMIND_ABOUT_EXPIRED = booleanPreferencesKey("remind_about_expired")
        val SNOOZE_ALL_INCLUDES_EXPIRING_SOON = booleanPreferencesKey("snooze_all_includes_expiring_soon")
        val ONLINE_PRODUCT_LOOKUP = booleanPreferencesKey("online_product_lookup")
        val SHOPPING_LIST_ID = longPreferencesKey("shopping_list_id")
        val DISMISSED_RESTOCK_SUGGESTIONS = stringSetPreferencesKey("dismissed_restock_suggestions")
        val SHOPPING_AISLE_ORDER = stringPreferencesKey("shopping_aisle_order")
        val KEEP_SCREEN_ON_WHILE_SHOPPING = booleanPreferencesKey("keep_screen_on_while_shopping")
        val AUTO_BACKUP_FOLDER = stringPreferencesKey("auto_backup_folder")
        val LAST_AUTO_BACKUP_AT = longPreferencesKey("last_auto_backup_at")
        val LAST_AUTO_BACKUP_FAILED = booleanPreferencesKey("last_auto_backup_failed")
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    }
}
