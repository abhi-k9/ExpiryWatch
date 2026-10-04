package io.github.abhik9.expirywatch.core.testing.repository

import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.model.UserSettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeUserSettingsRepository(initial: UserSettings = UserSettings()) : UserSettingsRepository {
    private val state = MutableStateFlow(initial)

    val current: UserSettings get() = state.value

    override val settings: Flow<UserSettings> = state

    override suspend fun setThemeMode(themeMode: ThemeMode) = state.update { it.copy(themeMode = themeMode) }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) =
        state.update { it.copy(useDynamicColor = useDynamicColor) }

    override suspend fun setExpiringSoonDays(days: Int) = state.update { it.copy(expiringSoonDays = days) }

    override suspend fun setRemindersEnabled(enabled: Boolean) = state.update { it.copy(remindersEnabled = enabled) }

    override suspend fun setReminderTime(time: LocalTime) = state.update { it.copy(reminderTime = time) }

    override suspend fun setExactReminders(exact: Boolean) = state.update { it.copy(exactReminders = exact) }

    override suspend fun setSortOrder(sortOrder: ItemSortOrder) = state.update { it.copy(sortOrder = sortOrder) }
}
