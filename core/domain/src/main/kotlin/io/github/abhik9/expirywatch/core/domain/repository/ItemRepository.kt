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

    /** Saves all of [items] like [upsert], or none of them if one fails. Returns their ids in order. */
    suspend fun upsertAll(items: List<Item>): List<Long>

    suspend fun updateStatus(id: Long, status: ItemStatus, finishedDate: LocalDate?)

    /**
     * Leaves the items out of reminders until [until] (see [Item.remindersSnoozedUntil]), or
     * includes them again when it's `null`.
     */
    suspend fun snoozeReminders(ids: Collection<Long>, until: LocalDate?)

    /** Puts the items in the group with [groupId], or takes them out of any group when it's `null`. */
    suspend fun moveToGroup(ids: Collection<Long>, groupId: Long?)

    suspend fun delete(id: Long)

    /** Puts a deleted [item] back, with its id. */
    suspend fun restore(item: Item)
}
