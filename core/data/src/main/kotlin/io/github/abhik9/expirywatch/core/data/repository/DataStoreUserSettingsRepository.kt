package io.github.abhik9.expirywatch.core.data.repository

import io.github.abhik9.expirywatch.core.datastore.UserSettingsDataSource
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ThemeMode
import io.github.abhik9.expirywatch.core.model.UserSettings
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class DataStoreUserSettingsRepository @Inject constructor(
    private val dataSource: UserSettingsDataSource,
) : UserSettingsRepository {
    override val settings: Flow<UserSettings> = dataSource.settings

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataSource.setThemeMode(themeMode)
    }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataSource.setUseDynamicColor(useDynamicColor)
    }

    override suspend fun setExpiringSoonDays(days: Int) {
        dataSource.setExpiringSoonDays(days)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        dataSource.setRemindersEnabled(enabled)
    }

    override suspend fun setReminderTime(time: LocalTime) {
        dataSource.setReminderTime(time)
    }

    override suspend fun setSortOrder(sortOrder: ItemSortOrder) {
        dataSource.setSortOrder(sortOrder)
    }
}
