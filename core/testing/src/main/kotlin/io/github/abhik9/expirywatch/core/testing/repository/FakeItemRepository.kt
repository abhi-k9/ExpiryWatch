package io.github.abhik9.expirywatch.core.testing.repository

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeItemRepository(initialItems: List<Item> = emptyList()) : ItemRepository {
    private val items = MutableStateFlow(initialItems.associateBy { it.id })
    private var nextId = (initialItems.maxOfOrNull { it.id } ?: 0) + 1

    val currentItems: List<Item> get() = items.value.values.toList()

    fun setItems(newItems: List<Item>) {
        items.value = newItems.associateBy { it.id }
        nextId = (newItems.maxOfOrNull { it.id } ?: 0) + 1
    }

    override fun observeActiveItems(): Flow<List<Item>> =
        items.map { all -> all.values.filter { it.status == ItemStatus.ACTIVE } }

    override fun observeFinishedItems(since: LocalDate): Flow<List<Item>> = items.map { all ->
        all.values.filter { item ->
            item.status != ItemStatus.ACTIVE && item.finishedDate?.let { it >= since } == true
        }
    }

    override fun observeItem(id: Long): Flow<Item?> = items.map { it[id] }

    override suspend fun getActiveItems(): List<Item> = currentItems.filter { it.status == ItemStatus.ACTIVE }

    override suspend fun upsert(item: Item): Long {
        val id = if (item.id == 0L) nextId++ else item.id
        items.update { it + (id to item.copy(id = id)) }
        return id
    }

    override suspend fun updateStatus(id: Long, status: ItemStatus, finishedDate: LocalDate?) {
        items.update { all ->
            val item = all[id] ?: return@update all
            all + (id to item.copy(status = status, finishedDate = finishedDate))
        }
    }

    override suspend fun delete(id: Long) {
        items.update { it - id }
    }
}
