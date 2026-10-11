package io.github.abhik9.expirywatch.core.testing.repository

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ShoppingHistoryEntry
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.model.ShoppingList
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeShoppingRepository : ShoppingRepository {
    private val lists = MutableStateFlow<List<ShoppingList>>(emptyList())
    private val items = MutableStateFlow<Map<Long, ShoppingItem>>(emptyMap())
    private val history = MutableStateFlow<Map<String, ShoppingHistoryEntry>>(emptyMap())
    private var nextListId = 1L
    private var nextItemId = 1L

    val currentLists: List<ShoppingList> get() = lists.value
    val currentItems: List<ShoppingItem> get() = items.value.values.toList()
    val currentHistory: List<ShoppingHistoryEntry> get() = history.value.values.toList()

    override fun observeLists(): Flow<List<ShoppingList>> = lists

    override suspend fun getLists(): List<ShoppingList> = lists.value

    override suspend fun createList(name: String, createdAt: Instant): Long {
        val id = nextListId++
        lists.update { it + ShoppingList(id = id, name = name, createdAt = createdAt) }
        return id
    }

    override suspend fun renameList(id: Long, name: String) =
        lists.update { all -> all.map { if (it.id == id) it.copy(name = name) else it } }

    override suspend fun deleteList(id: Long) {
        lists.update { all -> all.filterNot { it.id == id } }
        items.update { all -> all.filterValues { it.listId != id } }
    }

    override fun observeItemNames(): Flow<List<String>> = items.map { all -> all.values.map { it.name } }

    override fun observeToBuyCounts(): Flow<Map<Long, Int>> =
        items.map { all -> all.values.filter { !it.isChecked }.groupingBy { it.listId }.eachCount() }

    override fun observeItems(listId: Long): Flow<List<ShoppingItem>> =
        items.map { all -> all.values.filter { it.listId == listId } }

    override suspend fun getItems(listId: Long): List<ShoppingItem> = items.value.values.filter { it.listId == listId }

    override suspend fun upsertItem(item: ShoppingItem): Long {
        val id = if (item.id == 0L) nextItemId++ else item.id
        items.update { it + (id to item.copy(id = id)) }
        return id
    }

    override suspend fun upsertItems(items: List<ShoppingItem>): List<Long> = items.map { upsertItem(it) }

    override suspend fun setChecked(ids: Collection<Long>, checkedAt: Instant?) = items.update { all ->
        all.mapValues { (id, item) -> if (id in ids) item.copy(checkedAt = checkedAt) else item }
    }

    override suspend fun deleteItems(ids: Collection<Long>) = items.update { it - ids.toSet() }

    override suspend fun restoreItems(items: List<ShoppingItem>) {
        val listIds = lists.value.map { it.id }.toSet()
        val kept = items.filter { it.listId in listIds }
        this.items.update { all -> all + kept.associateBy { it.id } }
        nextItemId = maxOf(nextItemId, (kept.maxOfOrNull { it.id } ?: 0) + 1)
    }

    override fun observeHistory(): Flow<List<ShoppingHistoryEntry>> = history.map { all ->
        all.values.sortedWith(
            compareByDescending<ShoppingHistoryEntry> { it.timesAdded }.thenByDescending { it.lastAddedAt },
        )
    }

    override suspend fun findInHistory(name: String): ShoppingHistoryEntry? = history.value[productKey(name)]

    override suspend fun rememberAdded(name: String, category: Category?, addedAt: Instant) {
        val key = productKey(name)
        if (key.isEmpty()) return
        history.update { all ->
            val known = all[key]
            val entry = ShoppingHistoryEntry(
                name = name.trim(),
                category = category ?: known?.category,
                timesAdded = (known?.timesAdded ?: 0) + 1,
                lastAddedAt = addedAt,
            )
            all + (key to entry)
        }
    }

    override suspend fun rememberProduct(name: String, category: Category?, at: Instant) {
        val key = productKey(name)
        if (key.isEmpty()) return
        history.update { all ->
            val known = all[key]
            val entry = ShoppingHistoryEntry(
                name = name.trim(),
                category = category,
                timesAdded = known?.timesAdded ?: 1,
                lastAddedAt = known?.lastAddedAt ?: at,
            )
            all + (key to entry)
        }
    }
}
