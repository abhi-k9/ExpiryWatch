package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.StorageLocation
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    /** All categories, sorted by name. */
    fun observeCategories(): Flow<List<Category>>

    suspend fun upsert(category: Category): Long

    /** Deletes the category. Items in it are kept, but lose their category. */
    suspend fun delete(id: Long)
}

interface StorageLocationRepository {
    /** All storage locations, sorted by name. */
    fun observeLocations(): Flow<List<StorageLocation>>

    suspend fun upsert(location: StorageLocation): Long

    /** Deletes the location. Items in it are kept, but lose their location. */
    suspend fun delete(id: Long)
}

interface CustomGroupRepository {
    /** All of the user's groups, sorted by name. */
    fun observeGroups(): Flow<List<CustomGroup>>

    suspend fun upsert(group: CustomGroup): Long

    /** Deletes the group. Its items are kept, but leave the group. */
    suspend fun delete(id: Long)
}
