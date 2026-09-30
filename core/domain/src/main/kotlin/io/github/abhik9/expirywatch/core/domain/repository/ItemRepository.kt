package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    /** Items still in stock, in no particular order. */
    fun observeActiveItems(): Flow<List<Item>>

    /** Items used up or thrown away on or after [since]. */
    fun observeFinishedItems(since: LocalDate): Flow<List<Item>>

    fun observeItem(id: Long): Flow<Item?>

    suspend fun getActiveItems(): List<Item>

    /** Inserts a new item (when [Item.id] is 0) or updates an existing one. Returns its id. */
    suspend fun upsert(item: Item): Long

    suspend fun updateStatus(id: Long, status: ItemStatus, finishedDate: LocalDate?)

    suspend fun delete(id: Long)
}
