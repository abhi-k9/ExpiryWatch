package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ShoppingHistoryEntry
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.model.ShoppingList
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface ShoppingRepository {
    /** All shopping lists, oldest first. */
    fun observeLists(): Flow<List<ShoppingList>>

    suspend fun getLists(): List<ShoppingList>

    /** Returns the new list's id. */
    suspend fun createList(name: String, createdAt: Instant): Long

    suspend fun renameList(id: Long, name: String)

    /** Deletes the list together with everything on it. */
    suspend fun deleteList(id: Long)

    /** How many items are still to buy on each list, by list id. Lists with none are left out. */
    fun observeToBuyCounts(): Flow<Map<Long, Int>>

    /** The items on the list, in no particular order. */
    fun observeItems(listId: Long): Flow<List<ShoppingItem>>

    suspend fun getItems(listId: Long): List<ShoppingItem>

    /** Inserts a new item (when [ShoppingItem.id] is 0) or updates an existing one. Returns its id. */
    suspend fun upsertItem(item: ShoppingItem): Long

    /** Saves all of [items] like [upsertItem], or none of them if one fails. */
    suspend fun upsertItems(items: List<ShoppingItem>): List<Long>

    /** Puts the items in the cart at [checkedAt], or takes them out again when it's `null`. */
    suspend fun setChecked(ids: Collection<Long>, checkedAt: Instant?)

    suspend fun deleteItems(ids: Collection<Long>)

    /** Puts back items removed with [deleteItems], with the same ids, to undo it. */
    suspend fun restoreItems(items: List<ShoppingItem>)

    /** What was added to shopping lists before, most often added first. */
    fun observeHistory(): Flow<List<ShoppingHistoryEntry>>

    /** The history entry for the product called [name], ignoring case, accents and spacing. */
    suspend fun findInHistory(name: String): ShoppingHistoryEntry?

    /**
     * Remembers that [name] was added to a list at [addedAt]. Keeps the category remembered before
     * when [category] is `null`.
     */
    suspend fun rememberAdded(name: String, category: Category?, addedAt: Instant)

    /**
     * Remembers the product called [name] by that name and in [category], without counting it as
     * added again, as when an item on a list is renamed or moved to another category. A product
     * not added before is remembered as added once, at [at].
     */
    suspend fun rememberProduct(name: String, category: Category?, at: Instant)
}
