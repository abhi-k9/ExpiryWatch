package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.model.AutoBackupRun
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ProductGrouping
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

    suspend fun setProductGrouping(grouping: ProductGrouping)

    suspend fun setKeepProductsTogether(keepTogether: Boolean)

    suspend fun setRemindAboutExpired(remind: Boolean)

    suspend fun setOnlineProductLookup(enabled: Boolean)

    suspend fun setShoppingListId(id: Long)

    suspend fun setDismissedRestockSuggestions(keys: Set<String>)

    suspend fun setShoppingAisleOrder(categoryIds: List<Long>)

    suspend fun setKeepScreenOnWhileShopping(keepOn: Boolean)

    /** Sets the folder to back up to every week, or turns automatic backups off with `null`. */
    suspend fun setAutoBackupFolder(uri: String?)

    suspend fun setLastAutoBackup(run: AutoBackupRun)
}
