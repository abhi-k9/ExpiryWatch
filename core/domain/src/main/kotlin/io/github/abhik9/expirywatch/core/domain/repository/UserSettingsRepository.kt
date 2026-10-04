package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.model.UserSettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setUseDynamicColor(useDynamicColor: Boolean)

    suspend fun setExpiringSoonDays(days: Int)

    suspend fun setRemindersEnabled(enabled: Boolean)

    suspend fun setReminderTime(time: LocalTime)

    suspend fun setExactReminders(exact: Boolean)

    suspend fun setSortOrder(sortOrder: ItemSortOrder)
}
