package io.github.abhik9.expirywatch.core.data

import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.expirywatch.core.database.dao.CategoryDao
import io.github.abhik9.expirywatch.core.database.dao.ItemDao
import io.github.abhik9.expirywatch.core.database.dao.LocationDao
import io.github.abhik9.expirywatch.core.database.dao.ProductDao
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ItemStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** How much the user has stored, and their settings: counts only, not the data itself. */
internal class DataDiagnosticsSection @Inject constructor(
    private val itemDao: ItemDao,
    private val categoryDao: CategoryDao,
    private val locationDao: LocationDao,
    private val productDao: ProductDao,
    private val settingsRepository: UserSettingsRepository,
) : DiagnosticsSection {
    override val title = "Data"

    override suspend fun describe(): List<String> = listOf(
        "Items: ${itemDao.count(ItemStatus.ACTIVE)} active, ${itemDao.count(ItemStatus.CONSUMED)} used up, " +
            "${itemDao.count(ItemStatus.WASTED)} thrown away",
        "Categories: ${categoryDao.count()}, locations: ${locationDao.count()}, " +
            "remembered products: ${productDao.count()}",
        "Settings: ${settingsRepository.settings.first()}",
    )
}
