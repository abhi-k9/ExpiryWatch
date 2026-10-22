package io.github.abhik9.expirywatch.core.data

import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.expirywatch.core.database.dao.CategoryDao
import io.github.abhik9.expirywatch.core.database.dao.ItemDao
import io.github.abhik9.expirywatch.core.database.dao.LocationDao
import io.github.abhik9.expirywatch.core.database.dao.ProductDao
import io.github.abhik9.expirywatch.core.database.dao.ShoppingDao
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** How much the user has stored, and their settings: counts only, not the data itself. */
internal class DataDiagnosticsSection @Inject constructor(
    private val itemDao: ItemDao,
    private val categoryDao: CategoryDao,
    private val locationDao: LocationDao,
    private val productDao: ProductDao,
    private val shoppingDao: ShoppingDao,
    private val settingsRepository: UserSettingsRepository,
) : DiagnosticsSection {
    override val title = "Data"

    override suspend fun describe(): List<String> {
        val settings = settingsRepository.settings.first()
        return listOf(
            "Items: ${itemDao.count(ItemStatus.ACTIVE)} active, ${itemDao.count(ItemStatus.CONSUMED)} used up, " +
                "${itemDao.count(ItemStatus.WASTED)} thrown away",
            "Categories: ${categoryDao.count()}, locations: ${locationDao.count()}, " +
                "remembered products: ${productDao.count()}",
            "Shopping lists: ${shoppingDao.countLists()}, with ${shoppingDao.countItems()} items",
            "Settings: ${settings.withoutPersonalData()}",
            "Automatic backups: ${if (settings.autoBackupFolder != null) "on" else "off"}, " +
                "last run: ${settings.lastAutoBackup ?: "none"}",
            "Dismissed restock suggestions: ${settings.dismissedRestockSuggestions.size}",
        )
    }

    /**
     * The settings with what could identify the user's things left out: the backup folder's path
     * and the products whose restock suggestions were dismissed. Settings that hold such data
     * belong here too.
     */
    private fun UserSettings.withoutPersonalData() = copy(
        autoBackupFolder = null,
        lastAutoBackup = null,
        dismissedRestockSuggestions = emptySet(),
    )
}
