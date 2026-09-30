package io.github.abhik9.expirywatch.core.data.repository

import io.github.abhik9.expirywatch.core.database.dao.CategoryDao
import io.github.abhik9.expirywatch.core.database.dao.LocationDao
import io.github.abhik9.expirywatch.core.database.model.CategoryEntity
import io.github.abhik9.expirywatch.core.database.model.LocationEntity
import io.github.abhik9.expirywatch.core.database.model.asEntity
import io.github.abhik9.expirywatch.core.database.model.asExternalModel
import io.github.abhik9.expirywatch.core.domain.repository.CategoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.StorageLocationRepository
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.StorageLocation
import java.text.Collator
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll().map { categories ->
        categories.map(CategoryEntity::asExternalModel).sortedWith(compareBy(Collator.getInstance()) { it.name })
    }

    override suspend fun upsert(category: Category): Long = if (category.id == 0L) {
        categoryDao.insert(category.asEntity())
    } else {
        categoryDao.update(category.asEntity())
        category.id
    }

    override suspend fun delete(id: Long) = categoryDao.delete(id)
}

internal class RoomStorageLocationRepository @Inject constructor(
    private val locationDao: LocationDao,
) : StorageLocationRepository {
    override fun observeLocations(): Flow<List<StorageLocation>> = locationDao.observeAll().map { locations ->
        locations.map(LocationEntity::asExternalModel).sortedWith(compareBy(Collator.getInstance()) { it.name })
    }

    override suspend fun upsert(location: StorageLocation): Long = if (location.id == 0L) {
        locationDao.insert(location.asEntity())
    } else {
        locationDao.update(location.asEntity())
        location.id
    }

    override suspend fun delete(id: Long) = locationDao.delete(id)
}
