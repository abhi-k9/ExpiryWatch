package io.github.abhik9.expirywatch.core.testing.repository

import io.github.abhik9.expirywatch.core.domain.repository.CategoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.StorageLocationRepository
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.StorageLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    private val categories = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeCategories(): Flow<List<Category>> = categories.map { list -> list.sortedBy { it.name } }

    override suspend fun upsert(category: Category): Long {
        val id = if (category.id == 0L) nextId++ else category.id
        categories.update { list -> list.filterNot { it.id == id } + category.copy(id = id) }
        return id
    }

    override suspend fun delete(id: Long) {
        categories.update { list -> list.filterNot { it.id == id } }
    }
}

class FakeStorageLocationRepository(initial: List<StorageLocation> = emptyList()) : StorageLocationRepository {
    private val locations = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeLocations(): Flow<List<StorageLocation>> = locations.map { list -> list.sortedBy { it.name } }

    override suspend fun upsert(location: StorageLocation): Long {
        val id = if (location.id == 0L) nextId++ else location.id
        locations.update { list -> list.filterNot { it.id == id } + location.copy(id = id) }
        return id
    }

    override suspend fun delete(id: Long) {
        locations.update { list -> list.filterNot { it.id == id } }
    }
}
