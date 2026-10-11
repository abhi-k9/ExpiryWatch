package io.github.abhik9.expirywatch.core.data.repository

import androidx.room.withTransaction
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.database.ExpiryWatchDatabase
import io.github.abhik9.expirywatch.core.database.model.PopulatedShoppingHistory
import io.github.abhik9.expirywatch.core.database.model.PopulatedShoppingItem
import io.github.abhik9.expirywatch.core.database.model.ShoppingHistoryEntity
import io.github.abhik9.expirywatch.core.database.model.ShoppingListEntity
import io.github.abhik9.expirywatch.core.database.model.asEntity
import io.github.abhik9.expirywatch.core.database.model.asExternalModel
import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ShoppingHistoryEntry
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.model.ShoppingList
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Records changes by id only: what people shop for is personal data. */
internal class RoomShoppingRepository @Inject constructor(
    private val database: ExpiryWatchDatabase,
    private val log: EventLog,
) : ShoppingRepository {
    private val dao = database.shoppingDao()

    override fun observeLists(): Flow<List<ShoppingList>> =
        dao.observeLists().map { lists -> lists.map(ShoppingListEntity::asExternalModel) }

    override suspend fun getLists(): List<ShoppingList> = dao.getLists().map(ShoppingListEntity::asExternalModel)

    override suspend fun createList(name: String, createdAt: Instant): Long =
        dao.insertList(ShoppingListEntity(name = name, createdAt = createdAt))
            .also { id -> log.record { "shopping: created list #$id" } }

    override suspend fun renameList(id: Long, name: String) {
        dao.renameList(id, name)
        log.record { "shopping: renamed list #$id" }
    }

    override suspend fun deleteList(id: Long) {
        dao.deleteList(id)
        log.record { "shopping: deleted list #$id" }
    }

    override fun observeItemNames(): Flow<List<String>> = dao.observeItemNames()

    override fun observeToBuyCounts(): Flow<Map<Long, Int>> =
        dao.observeToBuyCounts().map { counts -> counts.associate { it.listId to it.count } }

    override fun observeItems(listId: Long): Flow<List<ShoppingItem>> =
        dao.observeItems(listId).map { items -> items.map(PopulatedShoppingItem::asExternalModel) }

    override suspend fun getItems(listId: Long): List<ShoppingItem> =
        dao.getItems(listId).map(PopulatedShoppingItem::asExternalModel)

    override suspend fun upsertItem(item: ShoppingItem): Long = if (item.id == 0L) {
        dao.insertItem(item.asEntity()).also { id -> log.record { "shopping: added #$id to list #${item.listId}" } }
    } else {
        dao.updateItem(item.asEntity())
        log.record { "shopping: updated #${item.id}" }
        item.id
    }

    override suspend fun upsertItems(items: List<ShoppingItem>): List<Long> =
        database.withTransaction { items.map { upsertItem(it) } }

    override suspend fun setChecked(ids: Collection<Long>, checkedAt: Instant?) {
        if (ids.isEmpty()) return
        dao.setCheckedAt(ids.toList(), checkedAt)
        log.record { "shopping: ${if (checkedAt == null) "unchecked" else "checked"} ${ids.joinToString { "#$it" }}" }
    }

    override suspend fun deleteItems(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        dao.deleteItems(ids.toList())
        log.record { "shopping: removed ${ids.joinToString { "#$it" }}" }
    }

    override suspend fun restoreItems(items: List<ShoppingItem>) {
        if (items.isEmpty()) return
        dao.insertOrReplaceItems(items.map { it.asEntity() })
        log.record { "shopping: put back ${items.joinToString { "#${it.id}" }}" }
    }

    override fun observeHistory(): Flow<List<ShoppingHistoryEntry>> =
        dao.observeHistory().map { entries -> entries.map(PopulatedShoppingHistory::asExternalModel) }

    override suspend fun findInHistory(name: String): ShoppingHistoryEntry? =
        dao.findInHistory(productKey(name))?.asExternalModel()

    override suspend fun rememberAdded(name: String, category: Category?, addedAt: Instant) {
        val key = productKey(name)
        if (key.isEmpty()) return
        database.withTransaction {
            val known = dao.findInHistory(key)
            dao.upsertHistory(
                ShoppingHistoryEntity(
                    productKey = key,
                    name = name.trim(),
                    categoryId = category?.id ?: known?.entry?.categoryId,
                    timesAdded = (known?.entry?.timesAdded ?: 0) + 1,
                    lastAddedAt = addedAt,
                ),
            )
        }
    }

    override suspend fun rememberProduct(name: String, category: Category?, at: Instant) {
        val key = productKey(name)
        if (key.isEmpty()) return
        database.withTransaction {
            val known = dao.findInHistory(key)?.entry
            dao.upsertHistory(
                ShoppingHistoryEntity(
                    productKey = key,
                    name = name.trim(),
                    categoryId = category?.id,
                    timesAdded = known?.timesAdded ?: 1,
                    lastAddedAt = known?.lastAddedAt ?: at,
                ),
            )
        }
    }
}
