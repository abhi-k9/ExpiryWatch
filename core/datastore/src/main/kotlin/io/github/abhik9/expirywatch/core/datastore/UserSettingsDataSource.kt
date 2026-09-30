package io.github.abhik9.expirywatch.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.model.UserSettings
import java.io.IOException
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

    suspend fun setSortOrder(sortOrder: ItemSortOrder) = dataStore.edit { it[Keys.SORT_ORDER] = sortOrder.name }

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
            sortOrder = enumOrDefault(this[Keys.SORT_ORDER], defaults.sortOrder),
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
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    }
}
